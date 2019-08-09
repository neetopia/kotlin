/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import com.bumptech.glide.annotation.Excludes
import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.types.KotlinType
import java.lang.IllegalStateException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import kotlin.collections.HashSet


class AppModuleGenerator(private val processingEnvironment: ProcessingEnvironment, private val processorUtil: ProcessorUtil) {

    companion object {
        val GENERATED_ROOT_MODULE_PACKAGE_NAME = "com.bumptech.glide"
        private val GLIDE_LOG_TAG = "Glide"
        private val GENERATED_APP_MODULE_IMPL_SIMPLE_NAME = "GeneratedAppGlideModuleImpl"
        private val GENERATED_ROOT_MODULE_SIMPLE_NAME = "GeneratedAppGlideModule"
    }

    fun generate(appGlideModule: KotlinType, libraryGlideModuleClassNames: Set<String>): TypeSpec {
        val appGlideModuleClassName = ClassName.bestGuess(appGlideModule.constructor.declarationDescriptor?.fqNameSafe?.toString()!!)
        val excludedGlideModuleClassNames = getExcludedGlideModuleClassNames(appGlideModule)
        val orderedLibraryGlideModuleClassNames = libraryGlideModuleClassNames.toList().sorted()
        val constructor = generateConstructor(
            appGlideModuleClassName,
            orderedLibraryGlideModuleClassNames,
            excludedGlideModuleClassNames
        )
        val registerComponents = generateRegisterComponents(
            orderedLibraryGlideModuleClassNames,
            excludedGlideModuleClassNames
        )
        val getExcludedModuleClasses = generateGetExcludedModuleClasses(excludedGlideModuleClassNames)
        val applyOptions = FunSpec.builder("applyOptions")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder("context", ClassName.bestGuess("android.content.Context"))
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("builder", ClassName.bestGuess("com.bumptech.glide.GlideBuilder"))
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addStatement("appGlideModule.applyOptions(context, builder)")
            .build()
        val isManifestParsingEnabled =
            FunSpec.builder("isManifestParsingEnabled")
                .addModifiers(KModifier.PUBLIC)
                .addModifiers(KModifier.OVERRIDE)
                .returns(Boolean::class)
                .addStatement("return appGlideModule.isManifestParsingEnabled()")
                .build()
        val builder = TypeSpec.classBuilder(GENERATED_APP_MODULE_IMPL_SIMPLE_NAME)
            .addModifiers(KModifier.INTERNAL)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class)
                    .addMember("%S", "deprecation")
                    .build()
            ).superclass(
                ClassName.bestGuess("$GENERATED_ROOT_MODULE_PACKAGE_NAME.$GENERATED_ROOT_MODULE_SIMPLE_NAME"))
            .addProperty("appGlideModule", appGlideModuleClassName, KModifier.PRIVATE, KModifier.FINAL)
            .addFunction(constructor)
            .addFunction(applyOptions)
            .addFunction(registerComponents)
            .addFunction(isManifestParsingEnabled)
            .addFunction(getExcludedModuleClasses)
        val generatedRequestManagerFactoryClassName = ClassName.bestGuess(
            "${RequestManagerFactoryGenerator.GENERATED_REQUEST_MANAGER_FACTORY_PACKAGE_NAME}." +
                    "${RequestManagerFactoryGenerator.GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME}"
        )
        builder.addFunction(
            FunSpec.builder("getRequestManagerFactory")
                .addModifiers(KModifier.OVERRIDE)
                .addAnnotation(processorUtil.nonNull())
                .returns(generatedRequestManagerFactoryClassName)
                .addStatement("return %T()", generatedRequestManagerFactoryClassName)
                .build()
        )
        return builder.build()
    }

    private fun generateGetExcludedModuleClasses(excludedClassNames: Collection<String>): FunSpec {
        val wildcardOfObject = WildcardTypeName.producerOf(Any::class.java)
        val classOfWildcardOfObject = Class::class.asClassName().parameterizedBy(wildcardOfObject)
        val setOfClassOfWildcardOfObject = Set::class.asClassName().parameterizedBy(classOfWildcardOfObject)
        val hashSetOfClassOfWildcardOfObject = HashSet::class.asClassName().parameterizedBy(classOfWildcardOfObject)
        val builder = FunSpec.builder("getExcludedModuleClasses")
            .addModifiers(KModifier.OVERRIDE)
            .addAnnotation(processorUtil.nonNull())
            .returns(setOfClassOfWildcardOfObject)

        if (excludedClassNames.isEmpty()) {
            builder.addStatement("return %T.emptySet()", Collections::class)
        } else {
            builder.addStatement(
                "%T excludedClasses = %T()",
                setOfClassOfWildcardOfObject,
                hashSetOfClassOfWildcardOfObject
            )
            for (excludedClassName in excludedClassNames) {
                // TODO: Remove this when we no longer support manifest parsing.
                // Using a Literal ($L) instead of a type ($T) to get a fully qualified import that allows
                // us to suppress deprecation warnings. Aimed at deprecated GlideModules.
                builder.addStatement("excludedClasses.add(%L::class)", excludedClassName)
            }
            builder.addStatement("return excludedClasses")
        }
        return builder.build()
    }

    private fun generateRegisterComponents(
        libraryGlideModuleClassNames: Collection<String>,
        excludedGlideModuleClassNames: Collection<String>
    ): FunSpec {
        val registerComponents = FunSpec.builder("registerComponents")
            .addModifiers(KModifier.PUBLIC)
            .addModifiers(KModifier.OVERRIDE)
            .addParameter(
                ParameterSpec.builder("context", ClassName.bestGuess("android.content.Context"))
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("glide", ClassName.bestGuess("com.bumptech.glide.Glide"))
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            ).addParameter(
                ParameterSpec.builder("registry", ClassName.bestGuess("com.bumptech.glide.Registry"))
                    .addAnnotation(processorUtil.nonNull())
                    .build()
            )
        for (glideModule in libraryGlideModuleClassNames) {
            if (excludedGlideModuleClassNames.contains(glideModule)) {
                continue
            }
            val moduleClassName = ClassName.bestGuess(glideModule)
            registerComponents.addStatement("%T().registerComponents(context, glide, registry)", moduleClassName)
        }
        // Order matters here. The AppGlideModule must be called last.
        registerComponents.addStatement("appGlideModule.registerComponents(context, glide, registry)")
        return registerComponents.build()
    }

    private fun doesAppGlideModuleConstructorAcceptContext(appGlideModule: ClassName): Boolean {
        return false
        /*
        val appGlideModuleType = processingEnvironment.elementUtils.getTypeElement(appGlideModule.reflectionName())
        for (enclosed in appGlideModuleType.enclosedElements) {
            if (enclosed.kind == ElementKind.CONSTRUCTOR) {
                val constructor = enclosed as ExecutableElement
                val parameters = constructor.parameters
                if (parameters.isEmpty()) {
                    return false
                } else if (parameters.size > 1) {
                    throw IllegalStateException(
                        "Constructor for $appGlideModule accepts too many parameters, it should accept no parameters or a single context"
                    )
                } else {
                    val parameterType = parameters[0].asType()
                    val contextType = processingEnvironment.elementUtils.getTypeElement("android.content.Context").asType()
                    if (!processingEnvironment.typeUtils.isSameType(parameterType, contextType)) {
                        throw IllegalStateException("Unrecognized type: $parameterType")
                    }
                    return true
                }
            }
        }
        return false
         */
    }

    private fun generateConstructor(
        appGlideModule: ClassName,
        libraryGlideModuleClassNames: Collection<String>,
        excludedGlideModuleClassNames: Collection<String>
    ): FunSpec {
        val constructorBuilder = FunSpec.constructorBuilder()
            .addModifiers(KModifier.PUBLIC)
            .addParameter(
                ParameterSpec.builder("context", ClassName.bestGuess("android.content.Context"))
                    .build()
            )
        if (doesAppGlideModuleConstructorAcceptContext(appGlideModule)) {
            constructorBuilder.addStatement("appGlideModule = %T(context)", appGlideModule)
        } else {
            constructorBuilder.addStatement("appGlideModule = %T()", appGlideModule)
        }
        val androidLogName = ClassName.bestGuess("android.util.Log")
        constructorBuilder.beginControlFlow("if (%T.isLoggable(%S, %T.DEBUG))", androidLogName, GLIDE_LOG_TAG, androidLogName)
        constructorBuilder.addStatement(
            "%T.d(%S, %S)",
            androidLogName,
            GLIDE_LOG_TAG,
            "Discovered AppGlideModule from annotation: $appGlideModule"
        )
        for (glideModule in libraryGlideModuleClassNames) {
            if (excludedGlideModuleClassNames.contains(glideModule)) {
                constructorBuilder.addStatement(
                    "%T.d(%S, %S)",
                    androidLogName,
                    GLIDE_LOG_TAG,
                    "AppGlideModule excludes LibraryGlideModule from annotation: $glideModule"
                )
            } else {
                constructorBuilder.addStatement(
                    "%T.d(%S, %S)",
                    androidLogName,
                    GLIDE_LOG_TAG,
                    "Discovered LibraryGlideModule from annotation: $glideModule"
                )
            }
        }
        constructorBuilder.endControlFlow()
        return constructorBuilder.build()
    }

    private fun getExcludedGlideModuleClassNames(appGlideModule: KotlinType): List<String> {
        return emptyList()
//        val names = processorUtil.findClassValuesFromAnnotationOnClassAsNames(appGlideModule, Excludes::class.java)
//        return names.sorted()
    }

}