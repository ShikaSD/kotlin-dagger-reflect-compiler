package me.shika.dagger.reflect.resolver

import me.shika.dagger.reflect.renderer.DaggerReflectRenderer
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
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

class DaggerReflectAnalysisHandler(private val outputDir: File) : AnalysisHandlerExtension {
    override fun doAnalysis(
        project: Project,
        module: ModuleDescriptor,
        projectContext: ProjectContext,
        files: Collection<KtFile>,
        bindingTrace: BindingTrace,
        componentProvider: ComponentProvider
    ): AnalysisResult? {
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

        return null
    }

    private fun processDescriptor(descriptor: ClassDescriptor) {
        val componentAnnotation = descriptor.annotations.findAnnotation(COMPONENT_FQ_NAME) ?: return

        val renderer = DaggerReflectRenderer(outputDir, descriptor)
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

    companion object {
        val COMPONENT_FQ_NAME = FqName(dagger.Component::class.qualifiedName!!)
        val COMPONENT_FACTORY_FQ_NAME = FqName(dagger.Component.Factory::class.qualifiedName!!)
        val COMPONENT_BUILDER_FQ_NAME = FqName(dagger.Component.Builder::class.qualifiedName!!)
    }
}
