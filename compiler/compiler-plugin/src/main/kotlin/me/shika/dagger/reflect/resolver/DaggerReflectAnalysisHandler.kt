package me.shika.dagger.reflect.resolver

import me.shika.dagger.reflect.CLASS_SHOULD_BE_INTERFACE
import me.shika.dagger.reflect.DaggerReflectErrors
import me.shika.dagger.reflect.ic.ICCache
import me.shika.dagger.reflect.renderer.DaggerReflectRenderer
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.container.ComponentProvider
import org.jetbrains.kotlin.container.get
import org.jetbrains.kotlin.context.ProjectContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.reportFromPlugin
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.classOrObjectRecursiveVisitor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.resolve.lazy.ResolveSession
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter.Companion.CLASSIFIERS
import org.jetbrains.kotlin.util.prefixIfNot
import java.io.File

class DaggerReflectAnalysisHandler(
    private val outputDir: File,
    private val icManifestOutputDir: File?
) : AnalysisHandlerExtension {
    private var generated = false

    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
        if (generated) {
            return null
        }

        outputDir.mkdirs()
        val icManifest = ICCache(project, module, icManifestOutputDir)

        val resolveSession = componentProvider.get<ResolveSession>()

        for (file in files) {
            if (file.virtualFilePath.startsWith(outputDir.absolutePath)) {
                continue
            }
            file.accept(
                classOrObjectRecursiveVisitor {
                    if (it.annotationEntries.isEmpty()) return@classOrObjectRecursiveVisitor

                    val descriptor = resolveSession.resolveToDescriptor(it)
                    if (descriptor !is ClassDescriptor) return@classOrObjectRecursiveVisitor

                    val path = processDescriptor(descriptor, bindingTrace)?.prefixIfNot(File.separator)
                    if (path != null) {
                        icManifest.recordGeneratedFile(
                            descriptor.classId!!,
                            outputDir.absolutePath + path
                        )
                    }
                }
            )
        }

        generated = true

        val newFiles = icManifest.recordChanges(files as MutableCollection<KtFile>)

        return when {
            bindingTrace.bindingContext.diagnostics.any { it.severity == Severity.ERROR } -> {
                AnalysisResult.compilationError(bindingTrace.bindingContext)
            }
            newFiles.isEmpty() -> null
            else -> {
                AnalysisResult.RetryWithAdditionalRoots(
                    bindingContext = bindingTrace.bindingContext,
                    moduleDescriptor = module,
                    additionalKotlinRoots = newFiles,
                    additionalJavaRoots = emptyList(),
                    addToEnvironment = true
                )
            }
        }
    }

    private fun processDescriptor(descriptor: ClassDescriptor, trace: BindingTrace): String? {
        val componentAnnotation = descriptor.annotations.findAnnotation(COMPONENT_FQ_NAME) ?: return null
        if (descriptor.reportIfClass(trace)) return null

        val renderer = DaggerReflectRenderer(outputDir, descriptor, componentAnnotation)
        val childClasses = descriptor.unsubstitutedMemberScope.getContributedDescriptors(kindFilter = CLASSIFIERS)

        val factory = childClasses.find {
            it is ClassDescriptor && it.annotations.hasAnnotation(COMPONENT_FACTORY_FQ_NAME)
        }

        if (factory != null) {
            factory as ClassDescriptor
            if (factory.reportIfClass(trace)) return null
            return renderer.generateFactory(factory)
        }

        val builder = childClasses.find {
            it is ClassDescriptor && it.annotations.hasAnnotation(COMPONENT_BUILDER_FQ_NAME)
        }

        if (builder != null) {
            builder as ClassDescriptor
            if (builder.reportIfClass(trace)) return null
            return renderer.generateBuilder(builder)
        }

        return renderer.generateDefaultBuilder()
    }

    private fun ClassDescriptor.reportIfClass(trace: BindingTrace): Boolean {
        return if (kind != ClassKind.INTERFACE) {
            trace.reportClass(this) {
                CLASS_SHOULD_BE_INTERFACE.on(it.getClassOrInterfaceKeyword()!!)
            }
            true
        } else {
            false
        }
    }

    private fun BindingTrace.reportClass(descriptor: ClassDescriptor, diagnosticFactory: (KtClass) -> Diagnostic) {
        (descriptor.findPsi() as? KtClass)?.let {
            reportFromPlugin(
                diagnosticFactory(it),
                DaggerReflectErrors
            )
        }
    }

    companion object {
        private val COMPONENT_FQ_NAME = FqName(dagger.Component::class.qualifiedName!!)
        private val COMPONENT_FACTORY_FQ_NAME = FqName(dagger.Component.Factory::class.qualifiedName!!)
        private val COMPONENT_BUILDER_FQ_NAME = FqName(dagger.Component.Builder::class.qualifiedName!!)
    }
}
