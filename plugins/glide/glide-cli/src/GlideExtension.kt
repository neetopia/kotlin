/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.intellij.openapi.project.Project
import com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import com.sun.tools.javac.util.Context
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.extensions.BeforeAnalyzeExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.typeUtil.supertypes
import org.jetbrains.kotlin.utils.SmartList


class CliGlideExtension(
    annotationFqName: String
) : AbstractGlideExtension(annotationFqName) {
}

//data class FoundIndexedClassNames(val glideModules: Set<String>, val extensions: Set<String>)

abstract class AbstractGlideExtension(open val annotationFqName: String) : BeforeAnalyzeExtension {

    private val baseClassName = "com.bumptech.glide.module.AppGlideModule"

    private val processingEnvironment = JavacProcessingEnvironment.instance(Context())


    override fun generateFiles(project: Project, files: Collection<KtFile>, bindingContext: BindingContext): List<KtFile>? {
        val factory = KtPsiFactory(project, true)
        val newFilesList = SmartList<KtFile>()
        val processList = SmartList<KotlinType>()

        for (file in files) {
            for (clazz in file.declarations) {
                if (clazz !is KtClassOrObject)
                    continue
                if (shouldProcess(clazz, bindingContext) && clazz.simpleType(bindingContext).isSubType(baseClassName)) {
                    processList.add(clazz.simpleType(bindingContext))
                }
            }
        }
        if (processList.size == 1) {
            newFilesList.addAll(process(processList[0], bindingContext, factory))
        }
        return newFilesList
    }

    private fun getGlideName(): String {
        return "GlideApp"
    }

    private fun KtClassOrObject.simpleType(bindingContext: BindingContext) =
        bindingContext.get(BindingContext.CLASS, this)!!.defaultType

    private fun process(type: KotlinType, bindingContext: BindingContext, factory: KtPsiFactory): List<KtFile> {
        val fqName = type.constructor.declarationDescriptor?.fqNameSafe

        val processorUtil = ProcessorUtil(processingEnvironment, bindingContext, type, factory)
        val appModuleGenerator = AppModuleProcessor(processingEnvironment, processorUtil)

        return appModuleGenerator.processModules(getGlideName())
    }

    private fun shouldProcess(ktClassOrObject: KtClassOrObject, bindingContext: BindingContext): Boolean {
        return ktClassOrObject.annotationEntries.any {
            bindingContext.get(BindingContext.ANNOTATION, it)?.fqName?.asString() == annotationFqName
        }
    }

    private fun SimpleType.isSubType(baseClass: String): Boolean {
        return supertypes().any {
            it.constructor.declarationDescriptor?.fqNameSafe?.asString() == baseClass
                    || (it as SimpleType).isSubType(baseClass)
        }
    }
}