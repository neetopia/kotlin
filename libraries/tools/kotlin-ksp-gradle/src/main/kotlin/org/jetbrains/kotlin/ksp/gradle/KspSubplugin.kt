/*
 * Copyright 2010-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.ksp.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.compile.AbstractCompile
import org.gradle.tooling.provider.model.ToolingModelBuilderRegistry
import org.jetbrains.kotlin.ksp.gradle.model.builder.KspModelBuilder
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.ksp.gradle.KspGradleSubplugin.Companion.KSP_CONFIGURATION_NAME
import org.jetbrains.kotlin.ksp.gradle.KspGradleSubplugin.Companion.getKspGeneratedClassesDir
import org.jetbrains.kotlin.ksp.gradle.KspGradleSubplugin.Companion.getKspGeneratedSourcesDir
import java.io.File
import javax.inject.Inject

class KspGradleSubplugin @Inject internal constructor(private val registry: ToolingModelBuilderRegistry) : Plugin<Project> {
    companion object {
        fun isEnabled(project: Project) = project.plugins.findPlugin(KspGradleSubplugin::class.java) != null

        val KSP_CONFIGURATION_NAME = "ksp"

        @JvmStatic
        fun getKspGeneratedClassesDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/classes/$sourceSetName")

        @JvmStatic
        fun getKspGeneratedSourcesDir(project: Project, sourceSetName: String) =
            File(project.project.buildDir, "generated/ksp/src/$sourceSetName")
    }

    override fun apply(project: Project) {
        project.extensions.create("ksp", KspExtension::class.java)
        project.configurations.create(KSP_CONFIGURATION_NAME)

        registry.register(KspModelBuilder())
    }
}

class KspKotlinGradleSubplugin : KotlinGradleSubplugin<AbstractCompile> {
    companion object {
        const val KSP_ARTIFACT_NAME = "kotlin-ksp"
    }

    override fun isApplicable(project: Project, task: AbstractCompile) = KspGradleSubplugin.isEnabled(project)

    override fun apply(
        project: Project,
        kotlinCompile: AbstractCompile,
        javaCompile: AbstractCompile?,
        variantData: Any?,
        androidProjectHandler: Any?,
        kotlinCompilation: KotlinCompilation<*>?
    ): List<SubpluginOption> {
        if (!KspGradleSubplugin.isEnabled(project)) return emptyList()

        val kspExtension = project.extensions.findByType(KspExtension::class.java) ?: return emptyList()

        val kspConfiguration: Configuration = project.configurations.findByName(KSP_CONFIGURATION_NAME) ?: return emptyList()
        val kspClasspath: FileCollection = project.files(kspConfiguration)

        kotlinCompile.dependsOn(kspConfiguration.buildDependencies)

        val options = mutableListOf<SubpluginOption>()

        options += FilesSubpluginOption("apclasspath", kspConfiguration)

        val sourceSetName = kotlinCompilation?.compilationName ?: "default"
        val generatedClassesDir = getKspGeneratedClassesDir(project, sourceSetName)
        val generatedSourcesDir = getKspGeneratedSourcesDir(project, sourceSetName)
        options += SubpluginOption("classes", generatedClassesDir.path)
        options += SubpluginOption("sources", generatedSourcesDir.path)

        if (javaCompile != null) {
            val generatedJavaSources = javaCompile.project.fileTree(generatedSourcesDir)
            generatedJavaSources.include("**/*.java")
            javaCompile.source(generatedJavaSources)
        }

        return options
    }

    override fun getCompilerPluginId() = "org.jetbrains.kotlin.ksp"
    override fun getPluginArtifact(): SubpluginArtifact =
        SubpluginArtifact(groupId = "org.jetbrains.kotlin", artifactId = KSP_ARTIFACT_NAME, version = javaClass.`package`.implementationVersion)
}
