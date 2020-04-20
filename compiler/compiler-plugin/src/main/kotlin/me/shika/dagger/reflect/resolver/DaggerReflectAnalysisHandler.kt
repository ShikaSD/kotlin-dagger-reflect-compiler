package me.shika.dagger.reflect.resolver

import me.shika.dagger.reflect.renderer.DaggerReflectRenderer
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.impl.PsiManagerImpl
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CLASSIFIERS
import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Logger

class DaggerReflectAnalysisHandler(private val outputDir: File) : AnalysisHandlerExtension {
    private var generated = false
    private val log = Logger.getLogger("test")

    init {
        log.addHandler(handler)
    }

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        log.info("Files $files, generated=$generated")
        if (generated) {
            generated = false
            return null
        }

        outputDir.deleteRecursively()
        outputDir.mkdirs()

        project.removeFilesFromLastCompilation(files as MutableCollection<KtFile>)

        val resolveSession = componentProvider.get<ResolveSession>()

        for (file in files) {
            file.accept(
                classOrObjectRecursiveVisitor {
                    if (it.annotationEntries.isEmpty()) return@classOrObjectRecursiveVisitor

                    val descriptor = resolveSession.resolveToDescriptor(it)
                    if (descriptor !is ClassDescriptor) return@classOrObjectRecursiveVisitor

                    processDescriptor(descriptor)
                }
            )
        }

        generated = true

        return AnalysisResult.RetryWithAdditionalRoots(
            bindingContext = bindingTrace.bindingContext,
            moduleDescriptor = module,
            additionalKotlinRoots = listOf(outputDir),
            additionalJavaRoots = emptyList(),
            addToEnvironment = true
        )
    }

    private fun processDescriptor(descriptor: ClassDescriptor) {
        val componentAnnotation = descriptor.annotations.findAnnotation(COMPONENT_FQ_NAME) ?: return

        val renderer = DaggerReflectRenderer(outputDir, descriptor, componentAnnotation)
        val childClasses = descriptor.unsubstitutedMemberScope.getContributedDescriptors(kindFilter = CLASSIFIERS)

        val factory = childClasses.find {
            it is ClassDescriptor && it.annotations.hasAnnotation(COMPONENT_FACTORY_FQ_NAME)
        }

        if (factory != null) {
            renderer.generateFactory(factory as ClassDescriptor)
            return
        }

        val builder = childClasses.find {
            it is ClassDescriptor && it.annotations.hasAnnotation(COMPONENT_BUILDER_FQ_NAME)
        }

        if (builder != null) {
            renderer.generateBuilder(builder as ClassDescriptor)
            return
        }

        renderer.generateDefaultBuilder()
    }

    override fun analysisCompleted(project: Project, module: ModuleDescriptor, bindingTrace: BindingTrace, files: Collection<KtFile>): AnalysisResult? {
        handler.close()
        return super.analysisCompleted(project, module, bindingTrace, files)
    }

    private fun Project.removeFilesFromLastCompilation(files: MutableCollection<KtFile>) {
        files.removeIf { file ->
            file.virtualFilePath.startsWith(outputDir.absolutePath).also { removed ->
                if (removed) {
                    dropFileCaches(file)
                }
            }
        }
    }

    private fun Project.dropFileCaches(file: KtFile) {
        (PsiManager.getInstance(this) as PsiManagerImpl).fileManager.setViewProvider(file.virtualFile, null)
    }

    companion object {
        val COMPONENT_FQ_NAME = FqName(dagger.Component::class.qualifiedName!!)
        val COMPONENT_FACTORY_FQ_NAME = FqName(dagger.Component.Factory::class.qualifiedName!!)
        val COMPONENT_BUILDER_FQ_NAME = FqName(dagger.Component.Builder::class.qualifiedName!!)

        private val handler = FileHandler("/Users/andreishikov/projects/test/dagger-reflect-compiler/test.log", true)
    }
}
