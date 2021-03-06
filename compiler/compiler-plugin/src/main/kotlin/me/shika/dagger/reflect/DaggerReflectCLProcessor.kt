package me.shika.dagger.reflect

import me.shika.dagger.reflect.DaggerReflectCLProcessor.Companion.PLUGIN_ID
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import org.jetbrains.kotlin.kapt.cli.KaptCliOption
import java.io.File

class DaggerReflectCLProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(OUTPUT_DIR_OPTION, INCREMENTAL_DIR_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_DIR_OPTION -> configuration.put(Keys.OUTPUT_DIR, File(value))
            INCREMENTAL_DIR_OPTION -> configuration.put(Keys.IC_OUTPUT_DIR, File(value))
        }
    }

    companion object {
        const val PLUGIN_ID = "me.shika.dagger-reflect"
        val OUTPUT_DIR_OPTION =
            CliOption(
                optionName = "outputDir",
                valueDescription = "<path>",
                description = "Resulting generated files",
                required = true,
                allowMultipleOccurrences = false
            )

        val INCREMENTAL_DIR_OPTION =
            CliOption(
                optionName = "icOutputDir",
                valueDescription = "<path>",
                description = "Temporary data for ic compilation",
                required = false,
                allowMultipleOccurrences = false
            )
    }
}

class KaptInterceptCLProcessor : CommandLineProcessor {
    private var enabled: Boolean = true
    init {
        try {
            Class.forName("org.jetbrains.kotlin.kapt.cli.KaptCliOption")
        } catch (e: ClassNotFoundException) {
            enabled = false
        }
    }

    override val pluginId: String = KaptCliOption.ANNOTATION_PROCESSING_COMPILER_PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = if (enabled) KaptCliOption.values().toList() else emptyList()

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        configuration.put(Keys.KAPT_ENABLED, true)
    }
}

object Keys {
    val OUTPUT_DIR = CompilerConfigurationKey.create<File>("$PLUGIN_ID.outputDir")
    val IC_OUTPUT_DIR = CompilerConfigurationKey.create<File>("$PLUGIN_ID.icOutputDir")
    val KAPT_ENABLED = CompilerConfigurationKey.create<Boolean>("$PLUGIN_ID.kaptEnabled")
}
