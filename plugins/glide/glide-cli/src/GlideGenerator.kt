/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.bumptech.glide.annotation.GlideExtension
import com.squareup.kotlinpoet.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.TypeElement

class GlideGenerator(private val processingEnvironment: ProcessingEnvironment, private val processorUtil: ProcessorUtil) {
    companion object {
        val GLIDE_QUALIFIED_NAME = "com.bumptech.glide.Glide"

        val REQUEST_MANAGER_QUALIFIED_NAME = "com.bumptech.glide.RequestManager"

        val SUPPRESS_LINT_PACKAGE_NAME = "android.annotation"
        val SUPPRESS_LINT_CLASS_NAME = "SuppressLint"
    }

    private val requestManagerType = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME)
    private val glideType = processingEnvironment.elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME)

    fun generate(generatedPackageName: String, glideName: String, generatedRequestManager: TypeSpec): TypeSpec {
        val glideBuilder = TypeSpec.classBuilder(glideName)
            .addKdoc(
                "The entry point for interacting with Glide for Applications\n" +
                        "\n" +
                        "<p>Includes all generated APIs from all\n" +
                        "{@link %T}s in source and dependent libraries.\n" +
                        "\n" +
                        "<p>This class is generated and should not be modified" +
                        "\n" +
                        "@see %T\n",
                GlideExtension::class.java,
                glideType
            ).addModifiers(KModifier.PUBLIC)
            .addFunction(
                FunSpec.constructorBuilder()
                    .addModifiers(KModifier.PRIVATE)
                    .build()
            )

        val companionObj = TypeSpec.companionObjectBuilder()
            .addFunctions(
                generateOverridesForGlideFunctions(generatedPackageName, generatedRequestManager)
            ).build()

        return glideBuilder
            .addType(companionObj)
            .build()
    }

    private fun generateOverridesForGlideFunctions(generatedPackageName: String, generatedRequestManager: TypeSpec): List<FunSpec> {
        return processorUtil.findStaticFunctions(glideType)
            .map { function ->
                if (processorUtil.returnTypeMatches(function, requestManagerType)) {
                    overrideGlideWithFunction(generatedPackageName, generatedRequestManager, function)
                } else {
                    overrideGlideStaticFunction(function)
                }
            }
    }

    private fun overrideGlideWithFunction(
        generatedPackageName: String, generatedRequestManager: TypeSpec, functiontoOverride: ExecutableElement
    ): FunSpec {
        val generatedRequestManagerClassName = ClassName.bestGuess("$generatedPackageName.${generatedRequestManager.name}")
        val parameters = processorUtil.getParameters(functiontoOverride)
        assert(parameters.size == 1) { "Expected size of 1, but got $functiontoOverride" }
        val parameter = parameters.single()
        val builder = FunSpec.builder(functiontoOverride.simpleName.toString())
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(JvmStatic::class)
            .addKdoc(processorUtil.generateSeeFunctionKDoc(functiontoOverride))
            .addParameters(parameters)
            .returns(generatedRequestManagerClassName)
            .addStatement(
                "return %T.%N(%L) as %T",
                glideType,
                functiontoOverride.simpleName.toString(),
                parameter.name,
                generatedRequestManagerClassName
            )
        return addReturnAnnotations(builder, functiontoOverride).build()
    }

    private fun overrideGlideStaticFunction(functionToOverride: ExecutableElement): FunSpec {
        val parameters = processorUtil.getParameters(functionToOverride)

        val element = processingEnvironment.typeUtils.asElement(functionToOverride.returnType) as? TypeElement

        val builder = FunSpec.builder(functionToOverride.simpleName.toString())
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(JvmStatic::class)
            .addKdoc(processorUtil.generateSeeFunctionKDoc(functionToOverride))
            .addParameters(parameters)
        addReturnAnnotations(builder, functionToOverride)

        val returnsValue = element != null
        if (returnsValue) {
            builder.returns(element!!.asClassName().copy(nullable = true))
        }
        var code = StringBuilder(if (returnsValue) "return " else "")
        code.append("%T.%N(")
        val args = ArrayList<Any>()
        args.add(glideType.asClassName())
        args.add(functionToOverride.simpleName)
        if (parameters.isNotEmpty()) {
            for (param in parameters) {
                code.append("%L, ")
                args.add(param.name)
            }
            code = java.lang.StringBuilder(code.dropLast(2))
        }
        code.append(")")
        builder.addStatement(code.toString(), *args.toArray())
        return builder.build()
    }

    private fun addReturnAnnotations(builder: FunSpec.Builder, functiontoOverride: ExecutableElement): FunSpec.Builder {
        val elements = processingEnvironment.elementUtils
        val visibleForTestingTypeElement = elements.getTypeElement(processorUtil.visibleForTesting().reflectionName())
        val visibleForTestingTypeQualifiedName = visibleForTestingTypeElement.toString()

        for (mirror in functiontoOverride.annotationMirrors) {
            builder.addAnnotation(AnnotationSpec.get(mirror))
            val annotationQuailifiedName = mirror.annotationType.toString()
            if (annotationQuailifiedName == visibleForTestingTypeQualifiedName) {
                builder.addAnnotation(
                    AnnotationSpec.builder(
                        ClassName.bestGuess("$SUPPRESS_LINT_PACKAGE_NAME.$SUPPRESS_LINT_CLASS_NAME"))
                        .addMember("%S", "VisibleForTests")
                        .build()
                )
            }
        }
        return builder
    }

}