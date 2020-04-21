package me.shika.dagger.reflect

import me.shika.dagger.reflect.resolver.DaggerReflectAnalysisHandler
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

class DaggerReflectPlugin : ComponentRegistrar {
    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val outputDir = configuration[Keys.OUTPUT_DIR]
            ?: throw IllegalArgumentException("Output directory is not specified")

        AnalysisHandlerExtension.registerExtension(project, DaggerReflectAnalysisHandler(outputDir))
    }

}
