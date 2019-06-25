/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.intellij.mock.MockProject
import org.jetbrains.kotlin.compiler.plugin.AbstractCliOption
import org.jetbrains.kotlin.compiler.plugin.CliOption
import org.jetbrains.kotlin.compiler.plugin.CommandLineProcessor
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.resolve.jvm.extensions.BeforeAnalyzeExtension

class GlideCommandLineProcessor : CommandLineProcessor {
    companion object {
        val ANNOTATION_OPTION = CliOption("annotation", "<fqname>", "Annotation qualified names",
                                          required = false, allowMultipleOccurrences = true)

        val PRESET_OPTION = CliOption("preset", "<name>", "Preset name balabala)",
                                      required = false, allowMultipleOccurrences = true)
        val PLUGIN_ID = "org.jetbrains.kotlin.glide"
    }

    override val pluginId = PLUGIN_ID

    override val pluginOptions = listOf(ANNOTATION_OPTION, PRESET_OPTION)

    override fun processOption(option: AbstractCliOption, value: String, configuration: CompilerConfiguration) {

    }
}

class GlideComponentRegistrar : ComponentRegistrar {

    override fun registerProjectComponents(project: MockProject, configuration: CompilerConfiguration) {
        val annotation = "com.bumptech.glide.annotation.GlideModule"
        BeforeAnalyzeExtension.registerExtension(project, CliGlideExtension(annotation))
    }
}