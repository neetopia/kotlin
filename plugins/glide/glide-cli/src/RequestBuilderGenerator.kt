/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideOption
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import java.io.File
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType

class RequestBuilderGenerator(
    private val processingEnvironment: ProcessingEnvironment,
    private val processorUtil: ProcessorUtil
) {
    companion object {
        val REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request"
        val REQUEST_OPTIONS_SIMPLE_NAME = "RequestOptions"
        val REQUEST_OPTIONS_QUALIFIED_NAME = "$REQUEST_OPTIONS_PACKAGE_NAME.$REQUEST_OPTIONS_SIMPLE_NAME"

        val REQUEST_BUILDER_PACKAGE_NAME = "com.bumptech.glide"
        val REQUEST_BUILDER_SIMPLE_NAME = "RequestBuilder"
        val REQUEST_BUILDER_QUALIFIED_NAME = "$REQUEST_BUILDER_PACKAGE_NAME.$REQUEST_BUILDER_SIMPLE_NAME"

        // Uses package private methods and variables.
        val GENERATED_REQUEST_BUILDER_SIMPLE_NAME = "GlideRequest"
    }

    private lateinit var generatedRequestBuilderClassName: ClassName
    private lateinit var requestOptionsClassName: ClassName
    private lateinit var generatedRequestBuilderOfTranscodeType: ParameterizedTypeName

    /**
     * An arbitrary name of the Generic type in the generated RequestBuilder. e.g.
     * RequestBuilder<TranscodeType>
     */
    private val TRANSCODE_TYPE_NAME = "TranscodeType"

    val requestBuilderType = processingEnvironment.getElementUtils().getTypeElement(REQUEST_BUILDER_QUALIFIED_NAME)

    val transcodeTypeName = TypeVariableName.invoke(TRANSCODE_TYPE_NAME)

    val requestOptionsType = processingEnvironment.getElementUtils().getTypeElement(REQUEST_OPTIONS_QUALIFIED_NAME)

    fun generate(generatedPackageName: String, glideExtensionClassNames: Set<String>, generatedOptions: TypeSpec?): TypeSpec {
        requestOptionsClassName =
            if (generatedOptions != null) {
                ClassName.bestGuess("$generatedPackageName.${generatedOptions.name}")
            } else {
                ClassName.bestGuess("${RequestOptionsGenerator.REQUEST_OPTIONS_PACKAGE_NAME}" +
                            ".${RequestOptionsGenerator.BASE_REQUEST_OPTIONS_SIMPLE_NAME}")
            }
        generatedRequestBuilderClassName = ClassName.bestGuess("$generatedPackageName.$GENERATED_REQUEST_BUILDER_SIMPLE_NAME")
        generatedRequestBuilderOfTranscodeType = generatedRequestBuilderClassName.parameterizedBy(transcodeTypeName)
        val requestBuilderOfTranscodeType = ClassName.bestGuess("$REQUEST_BUILDER_PACKAGE_NAME.$REQUEST_BUILDER_SIMPLE_NAME")
            .parameterizedBy(transcodeTypeName)


        return TypeSpec.classBuilder(GENERATED_REQUEST_BUILDER_SIMPLE_NAME)
            .addKdoc("Contains all public methods from {@link %T}, all options from\n", requestBuilderType)
            .addKdoc("{@link %T} and all generated options from\n", requestOptionsType)
            .addKdoc("{@link %T} in annotated methods in\n", GlideOption::class)
            .addKdoc("{@link %T} annotated classes.\n", GlideExtension::class)
            .addKdoc("\n")
            .addKdoc("<p>Generated code, do not modify.\n")
            .addKdoc("\n")
            .addKdoc("@see %T\n", requestBuilderType)
            .addKdoc("@see %T\n", requestOptionsType)
            .addAnnotation(
                AnnotationSpec.Companion.builder(SuppressWarnings::class)
                    .addMember("%S", "unused")
                    .addMember("%S", "deprecation")
                    .build()
            ).addModifiers(KModifier.PUBLIC)
            .addTypeVariable(transcodeTypeName)
            .superclass(requestBuilderOfTranscodeType)
            .addSuperinterface(Cloneable::class)
            .addFunctions(generateConstructors())
            .addFunction(generateDownloadOnlyRequestFunction())
            .addFunctions(generateRequestBuilderOverrides())
            .build()
    }

    private fun generateRequestBuilderOverrides(): List<FunSpec> {
        val rawRequestBuilderType = processingEnvironment.typeUtils.erasure(requestBuilderType.asType())
        return processorUtil.findInstanceFunctionReturning(requestBuilderType, rawRequestBuilderType)
            .map { generateRequestBuilderOverride(it) }
    }

    private fun generateRequestBuilderOverride(functionToOverride: ExecutableElement): FunSpec {
        val typeArgument = (functionToOverride.returnType as DeclaredType).typeArguments.first()
        val generatedRequestBuilderOfType = generatedRequestBuilderClassName.parameterizedBy(typeArgument.asTypeName())

        val builder = processorUtil.overriding(functionToOverride)
            .returns(generatedRequestBuilderOfType)
        builder.addCode(
            CodeBlock.builder()
                .add(
                    "return (%T) super.%N(",
                    generatedRequestBuilderOfType,
                    functionToOverride.simpleName)
                .add(builder.build().parameters.map { it.name }.joinToString(separator = ", "))
                .add(");\n")
                .build()
        )
        builder.addAnnotations(functionToOverride.annotationMirrors.map { AnnotationSpec.get(it) })
        return if (functionToOverride.isVarArgs) {
            builder.addModifiers(KModifier.FINAL)
                .addAnnotation(SafeVarargs::class.asTypeName())
                .addAnnotation(
                    AnnotationSpec.builder(SuppressWarnings::class)
                        .addMember("%S", "varargs")
                        .build()
                )
                .build()
        } else {
            builder.build()
        }
    }

    private fun generateConstructors(): List<FunSpec> {
        val classOfTranscodeType = ClassName.bestGuess(Class::class.java.canonicalName).parameterizedBy(transcodeTypeName)
        val wildcardOfObject = WildcardTypeName.producerOf(Unit::class)
        val requestBuilderOfWildcardOfObject = requestBuilderType.asClassName().parameterizedBy(wildcardOfObject)

        val firstConstructor = FunSpec.constructorBuilder()
            .addParameter(
                ParameterSpec.builder("transcodeClass", classOfTranscodeType)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("transcodeClass", requestBuilderOfWildcardOfObject)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addStatement("super(%N, %N)", "transcodeClass", "other")
            .build()
        val context = ClassName.bestGuess("android.content.Context")
        val glide = ClassName.bestGuess("com.bumptech.glide.Glide")
        val requestManager = ClassName.bestGuess("com.bumptech.glide.RequestManager")
        val secondConstructor = FunSpec.constructorBuilder()
            .addParameter(
                ParameterSpec.builder("glide", glide)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("requestManager", requestManager)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("transcodeClass", classOfTranscodeType)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("context", context)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addStatement(
                "super(%N, %N ,%N, %N)", "glide", "requestManager", "transcodeClass", "context")
            .build()
        return listOf(firstConstructor, secondConstructor)
    }

    private fun generateDownloadOnlyRequestFunction(): FunSpec {
        val generatedRequestBuilderOfFile = generatedRequestBuilderClassName.parameterizedBy(File::class.asTypeName())
        return FunSpec.builder("getDownloadOnlyRequest")
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(processorUtil.checkResult())
            .addAnnotation(processorUtil.nonNull())
            .returns(generatedRequestBuilderOfFile)
            .addModifiers(KModifier.PROTECTED)
            .addStatement(
                "return %T<>(%T.class, %N).apply(%N)",
                generatedRequestBuilderClassName,
                File::class,
                "this",
                "DOWNLOAD_ONLY_OPTIONS"
            ).build()
    }
}
