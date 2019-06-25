/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.codegen.AbstractBytecodeListingTest
import org.jetbrains.kotlin.resolve.jvm.extensions.BeforeAnalyzeExtension

abstract class AbstractBytecodeListingTestForGlide : AbstractBytecodeListingTest() {
    override fun setupEnvironment(environment: KotlinCoreEnvironment) {
        BeforeAnalyzeExtension.registerExtension(environment.project, CliGlideExtension("com.bumptech.glide.annotation.GlideModule"))
    }
}