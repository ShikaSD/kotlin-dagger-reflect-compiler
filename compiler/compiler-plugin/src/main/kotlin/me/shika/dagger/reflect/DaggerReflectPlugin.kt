package me.shika.dagger.reflect

import me.shika.dagger.reflect.resolver.DaggerReflectAnalysisHandler
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar.Companion.PLUGIN_COMPONENT_REGISTRARS
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class DaggerReflectPlugin : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val outputDir = configuration[Keys.OUTPUT_DIR]
            ?: throw IllegalArgumentException("Output directory is not specified")

        val hasKapt = configuration.getList(PLUGIN_COMPONENT_REGISTRARS).any { it.javaClass.name.contains("Kapt3") }

        if (!hasKapt) {
            AnalysisHandlerExtension.registerExtension(project, DaggerReflectAnalysisHandler(outputDir))
        }
    }

}
