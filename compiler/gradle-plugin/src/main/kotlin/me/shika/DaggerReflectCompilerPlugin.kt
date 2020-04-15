package me.shika

import me.shika.dagger.reflect.DaggerReflectCLProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinGradleSubplugin
import org.jetbrains.kotlin.gradle.plugin.SubpluginArtifact
import org.jetbrains.kotlin.gradle.plugin.SubpluginOption
import org.jetbrains.kotlin.gradle.tasks.AbstractKotlinCompile
import java.io.File

class DaggerReflectCompilerPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        // TODO verify enabled?
    }
}

class DaggerReflectCompilerSubplugin : KotlinGradleSubplugin<AbstractKotlinCompile<*>> {
    override fun apply(
        project: Project,
        kotlinCompile: AbstractKotlinCompile<*>,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<KotlinCommonOptions>?
    ): List<SubpluginOption> {
        val outputDirectory = File(project.buildDir, "generated/source/dagger-reflect/${kotlinCompile.name}")

        kotlinCompile.source(outputDirectory)
        kotlinCompile.exclude { it.file.startsWith(outputDirectory) }

        val module = project.extensions.findByType(IdeaModule::class.java)
        module?.generatedSourceDirs?.add(outputDirectory)

        return listOf(
            SubpluginOption(
                DaggerReflectCLProcessor.OUTPUT_DIR_OPTION.optionName,
                outputDirectory.toString()
            )
        )
    }

    override fun getCompilerPluginId(): String =
        DaggerReflectCLProcessor.PLUGIN_ID

    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact("me.shika", "dagger-reflect-compiler-plugin", "1.0.0-SNAPSHOT")

    override fun getNativeCompilerPluginArtifact(): SubpluginArtifact? {
        return super.getNativeCompilerPluginArtifact()
    }

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        task is AbstractKotlinCompile<*> && task !is KaptGenerateStubsTask
}
