package me.shika.dagger.reflect

import me.shika.dagger.reflect.DaggerReflectCLProcessor.Companion.PLUGIN_ID
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.CompilerConfigurationKey
import java.io.File

class DaggerReflectCLProcessor : CommandLineProcessor {
    override val pluginId: String = PLUGIN_ID
    override val pluginOptions: Collection<AbstractCliOption> = listOf(OUTPUT_DIR_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {
        when (option) {
            OUTPUT_DIR_OPTION -> configuration.put(Keys.OUTPUT_DIR, File(value))
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
    }
}

object Keys {
    val OUTPUT_DIR = CompilerConfigurationKey.create<File>("$PLUGIN_ID.outputDir")
}
