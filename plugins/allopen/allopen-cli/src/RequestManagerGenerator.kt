/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen
import com.bumptech.glide.annotation.GlideExtension
import com.bumptech.glide.annotation.GlideType
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ExecutableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeKind

class RequestManagerGenerator(
    private val processingEnvironment: ProcessingEnvironment,
    private val processorUtil: ProcessorUtil
) {
    companion object {
        val GLIDE_QUALIFIED_NAME = "com.bumptech.glide.Glide"
        val REQUEST_MANAGER_QUALIFIED_NAME = "com.bumptech.glide.RequestManager"
        val LIFECYCLE_QUALIFIED_NAME = "com.bumptech.glide.manager.Lifecycle"
        val REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME = "com.bumptech.glide.manager.RequestManagerTreeNode"
        val CONTEXT_CLASS_NAME = ClassName.bestGuess("android.content.Context")
        val GENERATED_REQUEST_MANAGER_SIMPLE_NAME = "GlideRequests"
    }

    private val lifecycleType = processingEnvironment.elementUtils.getTypeElement(LIFECYCLE_QUALIFIED_NAME)
    private val requestManagerTreeNodeType = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME)
    private val glideType = processingEnvironment.elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME)
    private val requestManagerType = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME)
    private val requestBuilderType = processingEnvironment.elementUtils.getTypeElement(RequestBuilderGenerator.REQUEST_BUILDER_QUALIFIED_NAME)
    private lateinit var generatedRequestBuilderClassName: ClassName
    private val requestManagerClassName = requestManagerType.asClassName()

    fun generate(
        generatedPackageName: String,
        requestOptions: TypeSpec?,
        requestBuilder: TypeSpec,
        glideExtensions: Set<String>
    ): TypeSpec {
        generatedRequestBuilderClassName = ClassName.bestGuess("$generatedPackageName.${requestBuilder.name}")
        return TypeSpec.classBuilder(GENERATED_REQUEST_MANAGER_SIMPLE_NAME)
            .superclass(requestManagerClassName)
            .addKdoc(
                "Includes all additions from methods in {@link %T}s\n" +
                        "annotated with {@link %T}\n" +
                        "\n" +
                        "<p>Generated code, do not modify\n",
                GlideExtension::class,
                GlideType::class
            ).addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class)
                    .addMember("%S", "deprecation")
                    .build()
            ).addModifiers(KModifier.PUBLIC)
            .addFunction(generateAsFunction())
            .addFunction(generateCallSuperConstructor())
            .addFunctions(generateExtensionRequestManagerFunctions(glideExtensions))
            .addFunctions(generateRequestManagerRequestManagerFunctionOverrides(generatedPackageName))
            .addFunctions(generateRequestManagerRequestBuilderFunctionOverrides())
            .addFunctions(listOf(generateOverrideSetRequestOptions(generatedPackageName, requestOptions)).filterNotNull())
            .build()
    }

    private fun generateCallSuperConstructor(): FunSpec {
        return FunSpec.constructorBuilder()
            .addModifiers(KModifier.PUBLIC)
            .addParameter(
                ParameterSpec.builder("glide", glideType.asClassName())
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("lifecycle", lifecycleType.asClassName())
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("treeNode", requestManagerTreeNodeType.asClassName())
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("context", CONTEXT_CLASS_NAME)
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).callSuperConstructor("glide", "lifecycle", "treeNode", "context")
            .build()
    }

    private fun generateAsFunction(): FunSpec {
        val resourceType = TypeVariableName.invoke("ResourceType")
        val classOfResourceType = Class::class.asTypeName().parameterizedBy(resourceType)
        val requestBuilderOfResourceType = generatedRequestBuilderClassName.parameterizedBy(resourceType)
        return FunSpec.builder("as")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(processorUtil.nonNull())
            .addAnnotation(processorUtil.checkResult())
            .addTypeVariable(resourceType)
            .returns(requestBuilderOfResourceType)
            .addParameter(ParameterSpec.builder("resourceClass", classOfResourceType.copy(false))
                              .addAnnotation(
                                  AnnotationSpec.builder(processorUtil.nonNull())
                                      .build()
                              ).build()
            ).addStatement("return %T<ResourceType>(glide, this, resourceClass, context)", generatedRequestBuilderClassName)
            .build()
    }

    private fun generateRequestManagerRequestManagerFunctionOverrides(generatedPackageName: String): List<FunSpec> {
        return processorUtil.findInstanceFunctionReturning(requestManagerType, requestManagerType).map {
            generateRequestManagerRequestManagerFunctionOverride(generatedPackageName, it)
        }
    }

    private fun generateRequestManagerRequestManagerFunctionOverride(generatedPackageName: String, function: ExecutableElement): FunSpec {
        val generatedRequestManagerName = ClassName.bestGuess("$generatedPackageName.$GENERATED_REQUEST_MANAGER_SIMPLE_NAME")
        val returns = processorUtil.overriding(function)
            .addAnnotation(processorUtil.nonNull())
            .returns(generatedRequestManagerName)

        return returns
            .addCode(processorUtil.generateCastingSuperCall(generatedRequestManagerName, returns.build()))
            .build()
    }

    private fun generateRequestManagerRequestBuilderFunctionOverrides(): List<FunSpec> {
        val rawRequestBuilder = processingEnvironment.typeUtils.erasure(requestBuilderType.asType())
        return processorUtil.findInstanceFunctionReturning(requestManagerType, rawRequestBuilder)
            .filterNot { it.simpleName.toString() == "as" }
            .map { generateRequestManagerRequestBuilderFunctionOverride(it) }
    }

    private fun generateRequestManagerRequestBuilderFunctionOverride(functionToOverride: ExecutableElement): FunSpec {
        val typeArgument = (functionToOverride.returnType as DeclaredType).typeArguments[0]
        val generatedRequestBuilderOfType = generatedRequestBuilderClassName.parameterizedBy(typeArgument.asTypeName())
        val builder = processorUtil.overriding(functionToOverride).returns(generatedRequestBuilderOfType)
        builder.addCode(processorUtil.generateCastingSuperCall(generatedRequestBuilderOfType, builder.build()))
        for (mirror in functionToOverride.annotationMirrors) {
            builder.addAnnotation(AnnotationSpec.get(mirror))
        }
        return builder.build()
    }

    private fun generateExtensionRequestManagerFunctions(glideExtensions: Set<String>): List<FunSpec> {
        val requestManagerExtensionFunctions = processorUtil.findAnnotatedElementsInClass(glideExtensions, GlideType::class.java)
        return requestManagerExtensionFunctions.map { generateAdditionalRequestManagerFunction(it) }
    }

    private fun generateAdditionalRequestManagerFunction(extensionFuntion: ExecutableElement): FunSpec {
        return if (extensionFuntion.returnType.kind == TypeKind.VOID) {
            generateAdditionalRequestManagerMethodLegacy(extensionFuntion)
        } else {
            generateAdditionalRequestManagerMethodNew(extensionFuntion)
        }
    }

    private fun generateAdditionalRequestManagerMethodLegacy(extensionFunction: ExecutableElement): FunSpec {
        val returnType = processorUtil.findClassValuesFromAnnotationOnClassAsNames(extensionFunction, GlideType::class.java)
            .iterator().next()
        val returnTypeClassName = ClassName.bestGuess(returnType)
        val parameterizedTypeName = generatedRequestBuilderClassName.parameterizedBy(returnTypeClassName)
        return FunSpec.builder(extensionFunction.simpleName.toString())
            .addModifiers(KModifier.PUBLIC)
            .returns(parameterizedTypeName)
            .addKdoc(processorUtil.generateSeeFunctionKDoc(extensionFunction))
            .addAnnotation(processorUtil.nonNull())
            .addAnnotation(processorUtil.checkResult())
            .addStatement("%T requestBuilder = this.`as`(%T.class)", parameterizedTypeName, returnTypeClassName)
            .addStatement("%T.%N(requestBuilder)", extensionFunction.enclosingElement, extensionFunction.simpleName)
            .addStatement("return requestBuilder")
            .build()
    }

    private fun generateAdditionalRequestManagerMethodNew(extensionFuntion: ExecutableElement): FunSpec {
        val returnType = processorUtil.findClassValuesFromAnnotationOnClassAsNames(extensionFuntion, GlideType::class.java)
            .iterator()
            .next()
        val returnTypeClassName = ClassName.bestGuess(returnType)
        val parameterizedTypeName = generatedRequestBuilderClassName.parameterizedBy(returnTypeClassName)

        return FunSpec.builder(extensionFuntion.getSimpleName().toString())
            .addModifiers(KModifier.PUBLIC)
            .returns(parameterizedTypeName)
            .addKdoc(processorUtil.generateSeeFunctionKDoc(extensionFuntion))
            .addAnnotation(processorUtil.nonNull())
            .addAnnotation(processorUtil.checkResult())
            .addStatement(
                "return (%T) %T.%N(this.as(%T.class))",
                parameterizedTypeName,
                extensionFuntion.enclosingElement,
                extensionFuntion.simpleName,
                returnTypeClassName
            ).build()
    }

    private fun generateOverrideSetRequestOptions(generatedPackageName: String, generatedRequestOptions: TypeSpec?): FunSpec? {
        if (generatedRequestOptions == null)
            return null
        val elements = processingEnvironment.elementUtils
        val requestOptionsType = elements.getTypeElement(RequestOptionsGenerator.REQUEST_OPTIONS_QUALIFIED_NAME)
        val generatedRequestOptionsQualifiedName = "$generatedPackageName.${generatedRequestOptions.name}"
        val functionName = "setRequestOptions"
        val parameterName = "toSet"
        return FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)
            .addModifiers(KModifier.PROTECTED)
            .addParameter(
                ParameterSpec.builder(parameterName, requestOptionsType.asClassName())
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).beginControlFlow("if (%N is %L)", parameterName, generatedRequestOptionsQualifiedName)
            .addStatement("super.%N(%N)", functionName, parameterName)
            .nextControlFlow("else")
            .addStatement("super.setRequestOptions(%L().apply(%N))", generatedRequestOptionsQualifiedName, parameterName)
            .endControlFlow()
            .build()
    }
}