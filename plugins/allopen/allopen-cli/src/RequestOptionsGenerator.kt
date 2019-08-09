/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen
import com.bumptech.glide.annotation.GlideExtension
import com.squareup.kotlinpoet.*
import com.sun.org.apache.xpath.internal.operations.Variable
import com.sun.tools.javac.processing.JavacProcessingEnvironment
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingContext
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror

class RequestOptionsGenerator(
    private val processingEnvironment: ProcessingEnvironment,
    private val processorUtil: ProcessorUtil
) {
    private val requestOptionsType = processingEnvironment.getElementUtils().getTypeElement("com.bumptech.glide.request.RequestOptions")
    private val GENERATED_REQUEST_OPTIONS_SIMPLE_NAME = "GlideOptions"
    private val requestOptionsOverrideGenerator = RequestOptionsOverrideGenerator(processingEnvironment, processorUtil)

    companion object {
        val REQUEST_OPTIONS_PACKAGE_NAME = "com.bumptech.glide.request"
        val REQUEST_OPTIONS_SIMPLE_NAME = "RequestOptions"
        val REQUEST_OPTIONS_QUALIFIED_NAME = "$REQUEST_OPTIONS_PACKAGE_NAME.$REQUEST_OPTIONS_SIMPLE_NAME"
        val BASE_REQUEST_OPTIONS_SIMPLE_NAME = "BaseRequestOptions"
        val BASE_REQUEST_OPTIONS_QUALIFIED_NAME = "$REQUEST_OPTIONS_PACKAGE_NAME.$BASE_REQUEST_OPTIONS_SIMPLE_NAME"
    }

    private lateinit var requestOptionsName: ClassName
    private lateinit var glideOptionsName: ClassName

    private var nextFieldId = 0

    fun generate(generatedPackagename: String, glideExtensionClassNames: Set<String>): TypeSpec {
        glideOptionsName = ClassName.bestGuess("$generatedPackagename.$GENERATED_REQUEST_OPTIONS_SIMPLE_NAME")
        requestOptionsName = ClassName.bestGuess("$REQUEST_OPTIONS_PACKAGE_NAME.$REQUEST_OPTIONS_SIMPLE_NAME")

        val staticOverrides = generateStaticMethodOverridesForRequestOptions()
        val allFunctionsAndStaticVars = ArrayList<FunctionAndStaticVar>()
        allFunctionsAndStaticVars.addAll(staticOverrides)

        val companionBuilder = TypeSpec.companionObjectBuilder()

        val classBuilder = TypeSpec.classBuilder(GENERATED_REQUEST_OPTIONS_SIMPLE_NAME)
            .addAnnotation(
                AnnotationSpec.builder(SuppressWarnings::class)
                    .addMember("%S", "deprecation")
                    .build()
            ).addKdoc(generateClassKDoc(glideExtensionClassNames))
            .addModifiers(KModifier.PUBLIC)
            .addSuperinterface(java.lang.Cloneable::class.java)
            .superclass(requestOptionsName)

        for (functionAndStaticVar in allFunctionsAndStaticVars) {
            functionAndStaticVar.function?.let { companionBuilder.addFunction(functionAndStaticVar.function) }
            functionAndStaticVar.staticVar?.let { companionBuilder.addProperty(functionAndStaticVar.staticVar) }
        }

        val instanceOverrides = requestOptionsOverrideGenerator.generateInstanceFunctionOverridesForRequestOptions(glideOptionsName)
        for (function in instanceOverrides) {
            classBuilder.addFunction(function)
        }

        classBuilder.addType(companionBuilder.build())

        return classBuilder.build()
    }

    private fun generateClassKDoc(glideExtensionClassNames: Set<String>): CodeBlock {
        val builder = CodeBlock.builder()
            .add(
                "Automatically generated from {@link %T} annotated classes.\n",
                GlideExtension::class)
            .add("\n")
            .add("@see %T\n", requestOptionsName)
        for (glideExtensionClass in glideExtensionClassNames) {
            builder.add("@see %T\n", ClassName.bestGuess(glideExtensionClass))
        }
        return builder.build()
    }


    private data class FunctionAndStaticVar(val function: FunSpec?, val staticVar: PropertySpec?)

    private fun generateStaticMethodOverridesForRequestOptions(): List<FunctionAndStaticVar> {

        val staticFunctionThatReturnRequestOptions = processorUtil.findStaticFunctionReturning(requestOptionsType, requestOptionsType)
        val staticFunctions = ArrayList<FunctionAndStaticVar>()

        for (element in staticFunctionThatReturnRequestOptions) {
            if (element.getAnnotation(Deprecated::class.java) != null) {
                continue
            }
            staticFunctions.add(generateStaticMethodEquivalentForRequestOptionsStaticMethod(element))
        }
        return staticFunctions
    }

    private fun generateStaticMethodEquivalentForRequestOptionsStaticMethod(staticMethod: ExecutableElement): FunctionAndStaticVar {
        val memorize = memorizeStaticMethodFromArguments(staticMethod)
        val staticMethodName = staticMethod.simpleName.toString()

        val equivalentInstanceMethodName = getInstanceMethodNameFromStaticMethodName(staticMethodName)

        val funSpecBuilder = FunSpec
            .builder(staticMethodName)
//            .addModifiers(KModifier.PUBLIC, KModifier.COMPANION)
            .addModifiers(KModifier.PUBLIC)
            .addAnnotation(JvmStatic::class)
            .addKdoc(processorUtil.generateSeeFunctionKDoc(staticMethod))
            .returns(glideOptionsName.copy(nullable = true))

        val createNewOptionAndCall = createNewOptionAndCall(
            memorize, funSpecBuilder, "%T().%N(", processorUtil.getParameters(staticMethod)
        )

        var requiredStaticProperty: PropertySpec? = null
        if (memorize) {
            val staticVariableName = staticMethodName + nextFieldId++
            requiredStaticProperty = PropertySpec.builder(staticVariableName, glideOptionsName.copy(nullable = true))
                .addModifiers(KModifier.PRIVATE)
                .mutable()
                .initializer("null")
//                .addModifiers(KModifier.COMPANION)
                .build()
            funSpecBuilder
                .beginControlFlow("if (%T.%N == null)", glideOptionsName, staticVariableName)
                .addStatement(
                    "%T.%N =\n$createNewOptionAndCall.%N()",
                    glideOptionsName,
                    staticVariableName,
                    glideOptionsName,
                    equivalentInstanceMethodName,
                    "autoClone"
                )
                .endControlFlow()
                .addStatement("return %T.%N", glideOptionsName, staticVariableName)
        } else {
            funSpecBuilder.addStatement(
                "return $createNewOptionAndCall", glideOptionsName, equivalentInstanceMethodName
            )
        }
        val typeParameters = staticMethod.typeParameters
        for (typeParameter in typeParameters) {
            funSpecBuilder.addTypeVariable(TypeVariableName.invoke(typeParameter.simpleName.toString()))
        }
        funSpecBuilder
            .addAnnotation(processorUtil.checkResult())
            .addAnnotation(processorUtil.nonNull())

        return FunctionAndStaticVar(funSpecBuilder.build(), requiredStaticProperty)
    }

    private fun createNewOptionAndCall(
        memorize: Boolean, funSpecBuilder: FunSpec.Builder, start: String, specs: List<ParameterSpec>
    ): StringBuilder {
        var createNewOptionAndCall = StringBuilder(start)
        if (specs.isNotEmpty()) {
            funSpecBuilder.addParameters(specs)
            for (parameter in specs) {
                createNewOptionAndCall.append(parameter.name)
                if (memorize && isAndroidContext(parameter)) {
                    createNewOptionAndCall.append(".getApplicationContext()")
                }
                createNewOptionAndCall.append(", ")
            }
            createNewOptionAndCall = StringBuilder(createNewOptionAndCall.dropLast(2))
        }
        createNewOptionAndCall.append(")")
        return createNewOptionAndCall
    }

    private fun isAndroidContext(parameter: ParameterSpec): Boolean {
        return parameter.type.toString() == "com.android.Context"
    }

    private fun getInstanceMethodNameFromStaticMethodName(staticMethodName: String): String {
        return when {
            staticMethodName == "bitmapTransform" -> "transform"
            staticMethodName == "decodeTypeOf" -> "decode"
            staticMethodName.endsWith("Transform") -> staticMethodName.substring(0, staticMethodName.length - 9)
            staticMethodName.endsWith("Of") -> staticMethodName.substring(0, staticMethodName.length - 2)
            staticMethodName == "noTransformation" -> "dontTransform"
            staticMethodName == "noAnimation" -> "dontAnimate"
            staticMethodName == "option" -> "set"
            else -> throw IllegalArgumentException("Unrecognized static method name: $staticMethodName")
        }
    }

    private fun memorizeStaticMethodFromArguments(staticMethod: ExecutableElement): Boolean {
        return staticMethod.parameters.isEmpty()
                || (staticMethod.parameters.size == 1
                && staticMethod.parameters.get(0).simpleName.toString() == "android.content.Context")
    }

}
