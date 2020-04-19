package me.shika

import me.shika.dagger.reflect.DaggerReflectCLProcessor
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.internal.KaptGenerateStubsTask
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
        // TODO verify enabled?
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
        val outputDirectory = File(project.buildDir, "generated/source/dagger-reflect/${kotlinCompile.name}")
        kotlinCompile.usePreciseJavaTracking = false

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

    override fun isApplicable(project: Project, task: AbstractCompile): Boolean =
        task is AbstractKotlinCompile<*> && task !is KaptGenerateStubsTask
}
