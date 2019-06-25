/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.squareup.kotlinpoet.*
import javax.annotation.processing.ProcessingEnvironment

class RequestManagerFactoryGenerator(val processingEnvironment: ProcessingEnvironment, private val processorUtil: ProcessorUtil) {
    companion object {
        val GLIDE_QUALIFIED_NAME = "com.bumptech.glide.Glide"
        val LIFECYCLE_QUALIFIED_NAME = "com.bumptech.glide.manager.Lifecycle"
        val REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME = "com.bumptech.glide.manager.RequestManagerTreeNode"
        val REQUEST_MANAGER_FACTORY_QUALIFIED_NAME = "com.bumptech.glide.manager.RequestManagerRetriever.RequestManagerFactory"
        val REQUEST_MANAGER_QUALIFIED_NAME = "com.bumptech.glide.RequestManager"
        val CONTEXT_CLASS_NAME = ClassName.bestGuess("android.content.Context")

        val GENERATED_REQUEST_MANAGER_FACTORY_PACKAGE_NAME = "com.bumptech.glide"
        val GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME = "GeneratedRequestManagerFactory"
    }

    val glideType = processingEnvironment.elementUtils.getTypeElement(GLIDE_QUALIFIED_NAME)
    val lifecycleType = processingEnvironment.elementUtils.getTypeElement(LIFECYCLE_QUALIFIED_NAME)
    val requestManagerTreeNodeType = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_TREE_NODE_QUALIFIED_NAME)
    val requestManagerFactoryInterface = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_FACTORY_QUALIFIED_NAME)
    val requestManagerClassName = processingEnvironment.elementUtils.getTypeElement(REQUEST_MANAGER_QUALIFIED_NAME).asClassName()

    fun generate(generatedPackageName: String, generatedRequestManagerSpec: TypeSpec): TypeSpec {
        return TypeSpec.classBuilder(GENERATED_REQUEST_MANAGER_FACTORY_SIMPLE_NAME)
            .addSuperinterface(requestManagerFactoryInterface.asClassName())
            .addKdoc("Generated code, do not modify\n")
            .addFunction(
                FunSpec.builder("build")
                    .addModifiers(KModifier.PUBLIC)
                    .addModifiers(KModifier.OVERRIDE)
                    .addAnnotation(processorUtil.nonNull())
                    .returns(requestManagerClassName)
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
                    ).addStatement(
                        "return %T(glide, lifecycle, treeNode, context)",
                        ClassName.bestGuess("$generatedPackageName.${generatedRequestManagerSpec.name}")
                    )
                    .build()
            )
            .build()
    }
}