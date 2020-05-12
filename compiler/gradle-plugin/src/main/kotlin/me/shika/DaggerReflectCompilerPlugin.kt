package me.shika

import com.android.build.gradle.api.BaseVariant
import me.shika.dagger.reflect.DaggerReflectCLProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.internal.KaptVariantData
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

class DaggerReflectCompilerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        target.apply { it.plugin(IdeaPlugin::class.java) }
    }
}

class DaggerReflectCompilerSubplugin : KotlinGradleSubplugin<KotlinCompile> {
    override fun apply(
        project: Project,
        kotlinCompile: KotlinCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        println("Applying dagger reflect on $project $kotlinCompile")

        val variant = when (variantData) {
            is BaseVariant -> variantData
            is KaptVariantData<*> -> variantData.variantData as BaseVariant
            else -> null
        }
        val outputDirectory = File(project.buildDir, "generated/source/dagger-reflect/${kotlinCompile.name}")
        val icOutputDirectory = File(project.buildDir, "kotlin/${kotlinCompile.name}/dagger-reflect/")

        project.extensions.findByType(IdeaModel::class.java)?.let { model ->
            model.apply {
                val isTest = kotlinCompile.name.contains("test", ignoreCase = true)
                if (!isTest ) {
                    module.sourceDirs = module.sourceDirs + outputDirectory
                    variant?.addJavaSourceFoldersToModel(outputDirectory)
                } else {
                    module.testSourceDirs = module.testSourceDirs + outputDirectory
                    variant?.addJavaSourceFoldersToModel(outputDirectory)
                }

                module.generatedSourceDirs = module.generatedSourceDirs + outputDirectory
            }
        }

        kotlinCompile.usePreciseJavaTracking = false
        return listOf(
            SubpluginOption(
                DaggerReflectCLProcessor.OUTPUT_DIR_OPTION.optionName,
                outputDirectory.toString()
            ),
            SubpluginOption(
                DaggerReflectCLProcessor.INCREMENTAL_DIR_OPTION.optionName,
                icOutputDirectory.toString()
            )
        )
    }

    override fun getCompilerPluginId(): String =
        DaggerReflectCLProcessor.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact("me.shika", "dagger-reflect-compiler-plugin", "1.0.1-SNAPSHOT")

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        project.isPluginEnabled && task is AbstractKotlinCompile<*> && task !is KaptGenerateStubsTask

    private val Project.isPluginEnabled
        get() = project.plugins.findPlugin(DaggerReflectCompilerPlugin::class.java) != null
}
