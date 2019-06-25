/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.extensions

import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext

interface BeforeAnalyzeExtension {
    companion object : ProjectExtensionDescriptor<BeforeAnalyzeExtension> (
        "org.jetbrains.kotlin.beforeAnalyzeExtension",
        BeforeAnalyzeExtension::class.java
    )

    fun generateFiles(
        project: Project,
        files: Collection<KtFile>,
        bindingContext: BindingContext
    ): List<KtFile>? = null
}