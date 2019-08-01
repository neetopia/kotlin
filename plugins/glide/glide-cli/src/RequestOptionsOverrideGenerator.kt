package org.jetbrains.kotlin.glide

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.glide.ProcessorUtil
import org.jetbrains.kotlin.glide.RequestOptionsGenerator.Companion.BASE_REQUEST_OPTIONS_QUALIFIED_NAME
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement

/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

class RequestOptionsOverrideGenerator(private val processingEnvironment: ProcessingEnvironment, private val processorUtil: ProcessorUtil) {
    private val baseRequestOptionsType = processingEnvironment.elementUtils.getTypeElement(BASE_REQUEST_OPTIONS_QUALIFIED_NAME)

    fun generateInstanceFunctionOverridesForRequestOptions(typeToOverrideIn: TypeName): List<FunSpec> {
        return generateInstanceFunctionOverridesForRequestOptions(typeToOverrideIn, emptySet<String>())
    }

    private fun generateInstanceFunctionOverridesForRequestOptions(typeToOverrideIn: TypeName, excludedFunctions: Set<String>): List<FunSpec> {
        return processorUtil.findInstanceFunctionReturning(baseRequestOptionsType, baseRequestOptionsType)
            .filterNot { excludedFunctions.contains(it.simpleName.toString()) }
            .map { generateRequestOptionOverride(typeToOverrideIn, it) }
    }

    private fun generateRequestOptionOverride(typeToOverrideIn: TypeName, functionToOverride: ExecutableElement): FunSpec {
        val result = processorUtil.overriding(functionToOverride).returns(typeToOverrideIn)
        result.addCode(
            CodeBlock.builder()
                .add("return super.%N(", functionToOverride.simpleName)
                .add(result.build().parameters.map{ it.name }.joinToString(separator = ", "))
                .add(") as %T\n", typeToOverrideIn)
                .build()
        )
        if (functionToOverride.simpleName.contains("transform") && functionToOverride.isVarArgs) {
            result.addAnnotation(SafeVarargs::class)
                .addAnnotation(
                    AnnotationSpec.builder(SuppressWarnings::class)
                        .addMember("%S", "varargs")
                        .build()
                )
        }
        for (mirror in functionToOverride.annotationMirrors) {
            result.addAnnotation(AnnotationSpec.get(mirror))
        }

        return result.build()
    }
}