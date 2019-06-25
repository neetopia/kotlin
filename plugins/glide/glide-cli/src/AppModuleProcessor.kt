/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.glide

import com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import javax.annotation.processing.ProcessingEnvironment

class AppModuleProcessor(private val processingEnvironment: ProcessingEnvironment, private val processorUtil: ProcessorUtil) {

    fun processModules(glideName: String): List<KtFile> {

        val glideGenPackage = processingEnvironment.elementUtils.getPackageElement(
            GlideAnnotationProcessor::class.java.`package`.name
        )
        val generatedPackageName = processorUtil.type.constructor.declarationDescriptor?.fqNameSafe?.parent().toString()
        val result = ArrayList<KtFile>()
        val requestOptionsGenerator = RequestOptionsGenerator(processingEnvironment, processorUtil)
        val requestBuilderGenerator = RequestBuilderGenerator(processingEnvironment, processorUtil)
        val requestManagerGenerator = RequestManagerGenerator(processingEnvironment, processorUtil)
        val requestManagerFactoryGenerator = RequestManagerFactoryGenerator(processingEnvironment, processorUtil)
        val glideGenerator = GlideGenerator(processingEnvironment, processorUtil)
        val appModuleGenerator = AppModuleGenerator(processingEnvironment, processorUtil)

        val glideExtensionClassNames = HashSet<String>()
        val glideLibraryClassNames = HashSet<String>()

        val generatedRequestOptions = requestOptionsGenerator.generate(generatedPackageName, glideExtensionClassNames)
        writeRequestOptions(generatedPackageName, generatedRequestOptions)

        val generatedRequestBuilder = requestBuilderGenerator.generate(generatedPackageName, glideExtensionClassNames, generatedRequestOptions)
        writeRequestBuilder(generatedPackageName, generatedRequestBuilder)

        val generatedRequestManager = requestManagerGenerator
            .generate(generatedPackageName, generatedRequestOptions, generatedRequestBuilder, glideExtensionClassNames)
        writeRequestManager(generatedPackageName, generatedRequestManager)

        val generatedRequestManagerFactory = requestManagerFactoryGenerator.generate(generatedPackageName, generatedRequestManager)
        writeRequestManagerFactory(generatedRequestManagerFactory)

        val generatedGlide = glideGenerator.generate(generatedPackageName, glideName, generatedRequestManager)
        writeGlide(generatedPackageName, generatedGlide)

        val generatedAppGlideModule = appModuleGenerator.generate(
            processorUtil.type,
            glideLibraryClassNames
        )
        writeAppModule(generatedAppGlideModule)

        return result
    }


    private fun writeRequestOptions(generatedPackageName: String, generatedRequestOptions: TypeSpec) {
        processorUtil.writeClass(generatedPackageName, generatedRequestOptions)
    }

    private fun writeRequestBuilder(generatedPackageName: String, generatedRequestBuilder: TypeSpec) {
        processorUtil.writeClass(generatedPackageName, generatedRequestBuilder)
    }

    private fun writeRequestManager(generatedPackageName: String, generatedRequestManager: TypeSpec) {
        processorUtil.writeClass(generatedPackageName, generatedRequestManager)
    }

    private fun writeRequestManagerFactory(requestManagerFactory: TypeSpec) {
        processorUtil.writeClass(AppModuleGenerator.GENERATED_ROOT_MODULE_PACKAGE_NAME, requestManagerFactory)
    }

    private fun writeAppModule(appModule: TypeSpec) {
        processorUtil.writeClass(AppModuleGenerator.GENERATED_ROOT_MODULE_PACKAGE_NAME, appModule)
    }

    private fun writeGlide(generatedPackageName: String, glide: TypeSpec) {
        processorUtil.writeClass(generatedPackageName, glide)
    }

//    TODO: investigate why we need @Index annotation and how we can by pass package level access limit
//    private fun getIndexedClassNames(glideGenPackage: PackageElement) {
//        val glideModules = HashSet<String>()
//        val extensions = HashSet<String>()
//
//        val glideGeneratedElements = glideGenPackage.enclosedElements
//        for (indexer in glideGeneratedElements) {
//            val annotation = indexer.getAnnotation(Index.class)
//        }
//    }
}