/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen

import com.bumptech.glide.annotation.compiler.GlideAnnotationProcessor
import com.squareup.kotlinpoet.*
import com.sun.tools.javac.code.Attribute
import com.sun.tools.javac.code.Type
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.types.KotlinType
import java.io.File
import java.lang.IllegalArgumentException
import java.lang.reflect.InvocationTargetException
import java.util.*
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.*
import javax.lang.model.type.TypeMirror
import kotlin.collections.ArrayList

class ProcessorUtil(
    private val processingEnvironment: ProcessingEnvironment,
    val bindingContext: BindingContext,
    val type: KotlinType,
    private val factory: KtPsiFactory
) {
    private val GLIDE_MODULE_PACKAGE_NAME = "com.bumptech.glide.module"
    private val APP_GLIDE_MODULE_SIMPLE_NAME = "AppGlideModule"
    private val LIBRARY_GLIDE_MODULE_SIMPLE_NAME = "LibraryGlideModule"
    private val APP_GLIDE_MODULE_QUALIFIED_NAME = "$GLIDE_MODULE_PACKAGE_NAME.$APP_GLIDE_MODULE_SIMPLE_NAME"
    private val LIBRARY_GLIDE_MODULE_QUALIFIED_NAME = "$GLIDE_MODULE_PACKAGE_NAME.$LIBRARY_GLIDE_MODULE_SIMPLE_NAME"
    private val COMPILER_PACKAGE_NAME = GlideAnnotationProcessor::class.java.getPackage().name

    private val SUPPORT_NONNULL_ANNOTATION = ClassName.bestGuess("android.support.annotation.NonNull")
    private val JETBRAINS_NOTNULL_ANNOTATION = ClassName.bestGuess("org.jetbrains.annotations.NotNull")
    private val ANDROIDX_NONNULL_ANNOTATION = ClassName.bestGuess("androidx.annotation.NonNull")
    private val SUPPORT_CHECK_RESULT_ANNOTATION = ClassName.bestGuess("android.support.annotation.CheckResult")
    private val ANDROIDX_CHECK_RESULT_ANNOTATION = ClassName.bestGuess("androidx.annotation.CheckResult")
    private val SUPPORT_VISIBLE_FOR_TESTING = ClassName.bestGuess("android.support.annotation.VisibleForTesting")
    private val ANDROIDX_VISIBLE_FOR_TESTING = ClassName.bestGuess("androidx.annotation.VisibleForTesting")

    fun writeClass(packageName: String, clazz: TypeSpec) {
        try {
            val fileSpec = FileSpec.builder(packageName, clazz.name!!)
                .addType(clazz)
                .build()
            fileSpec.writeTo(File("/usr/local/google/home/jiaxiang/code/tachiyomi/app/build/generated"))
        } catch (e: Exception) {
            throw e
        }
    }

//    fun writeClass(packageName: String, clazz: TypeSpec): KtFile {
//        return factory.createFile(
//            "${clazz.name!!}.kt",
//            FileSpec
//                .builder(packageName, clazz.name!!)
//                .build()
//                .toString()
//        )
//    }

    fun findAnnotatedElementsInClass(
        classNames: Set<String>,
        annotationClass: Class<out Annotation>
    ): List<ExecutableElement> {
        val result = ArrayList<ExecutableElement>()
        for (glideExtensionClassName in classNames) {
            val glideExtension = processingEnvironment.elementUtils.getTypeElement(glideExtensionClassName)
            for (element in glideExtension.enclosedElements) {
                if (element.getAnnotation(annotationClass) != null) {
                    result.add(element as ExecutableElement)
                }
            }
        }
        return result
    }

    fun findInstanceFunctionReturning(clazz: TypeElement, returnType: TypeMirror): List<ExecutableElement> {
        return clazz.enclosedElements.filter {
            it.isPublicMethod() && it.isInstanceMethod() && it.returnTypeMatches(returnType)
        }.map { it as ExecutableElement }
    }

    fun findInstanceFunctionReturning(clazz: TypeElement, returnType: TypeElement): List<ExecutableElement> {
        return clazz.enclosedElements.filter {
            it.isPublicMethod() && it.isInstanceMethod() && it.returnTypeMatches(returnType.asType())
        }.map { it as ExecutableElement }
    }

    fun findStaticFunctionReturning(clazz: TypeElement, returnType: TypeElement): List<ExecutableElement> {
        return clazz.enclosedElements.filter {
            it.isPublicMethod() && it.isStaticMethod() && it.returnTypeMatches(returnType.asType())
        }.map { it as ExecutableElement }
    }

    fun findStaticFunctions(clazz: TypeElement): List<ExecutableElement> {
        return clazz.enclosedElements.filter {
            it.isPublicMethod() && it.isStaticMethod()
        }.map { it as ExecutableElement }
    }

    private fun Element.isPublicMethod() =
        this.kind == ElementKind.METHOD && this.modifiers.contains(Modifier.PUBLIC)

    private fun Element.isStaticMethod() =
        this.kind == ElementKind.METHOD && this.modifiers.contains(Modifier.STATIC)

    private fun Element.isInstanceMethod() =
        this.kind == ElementKind.METHOD && !this.modifiers.contains(Modifier.STATIC)

    fun findClassValuesFromAnnotationOnClassAsNames(clazz: Element, annotationClass: Class<out Annotation>): Set<String> {
        var excludedModuleAnnotationValue: AnnotationValue? = null
        val annotationClassName = annotationClass.name
        for (annotationMirror in clazz.annotationMirrors) {
            if (annotationClassName != annotationMirror.annotationType.toString()) {
                continue
            }
            val values = annotationMirror.elementValues.entries
            if (values.size != 1) {
                throw IllegalArgumentException("Expected single value, but found: $values")
            }
            excludedModuleAnnotationValue = values.iterator().next().value
            if (excludedModuleAnnotationValue == null || excludedModuleAnnotationValue is Attribute.UnresolvedClass) {
                throw IllegalArgumentException("Failed to find value for: $annotationClass from mirrors: ${clazz.annotationMirrors}")
            }
        }
        if (excludedModuleAnnotationValue == null) {
            return Collections.emptySet()
        }
        if (excludedModuleAnnotationValue.value is com.sun.tools.javac.util.List<*>) {
            val values = excludedModuleAnnotationValue.value as List<*>
            val result = HashSet<String>()
            for (current in values) {
                result.add(getExcludedModuleClassFromAnnotationAttribute(clazz, current!!))
            }
            return result
        } else {
            return setOf((excludedModuleAnnotationValue.value as Type.ClassType).toString())
        }
    }

    private fun getExcludedModuleClassFromAnnotationAttribute(clazz: Element, attribute: Any): String {
        if (attribute.javaClass.simpleName == "UnresolvedClass") {
            throw IllegalArgumentException(
                "Failed to parse @Excludes for: $clazz" +
                        ", one or more excluded Modules could not be found at compile time. Ensure that all" +
                        "excluded Modules are included in your classpath.")
        }
        val methods = attribute.javaClass.declaredMethods
        if (methods == null || methods.isEmpty()) {
            throw IllegalArgumentException("Failed to parse @Excludes for: $clazz invalid exclude: $attribute")
        }
        for (method in methods) {
            if (method.name == "getValue") {
                try {
                    return method.invoke(attribute).toString()
                } catch (e: Exception) {
                    when (e) {
                        is IllegalAccessException , is InvocationTargetException -> {
                            throw IllegalArgumentException("Failed to parse @Excludes for: $clazz", e)
                        }
                        else -> {
                        }
                    }
                }
            }
        }
        throw IllegalArgumentException("Failed to parse @Excludes for: $clazz")
    }

    private fun Element.returnTypeMatches(expectedType: TypeMirror) =
        this is ExecutableElement && processingEnvironment.typeUtils.isAssignable(returnType, expectedType)

    fun returnTypeMatches(function: ExecutableElement, expectedType: TypeElement) = function.returnTypeMatches(expectedType.asType())

    fun generateSeeFunctionKDoc(function: ExecutableElement): CodeBlock {
        return generateSeeFunctionKDoc(
            getKDocSafeName(function.enclosingElement),
            function.simpleName.toString(),
            function.parameters
        )
    }

    fun generateSeeFunctionKDoc(
        nameOfClassContainingFunction: TypeName,
        functionSimpleName: String,
        functionParameters: List<out VariableElement>
    ): CodeBlock {
        return generateSeeFunctionKDocInternal(
            nameOfClassContainingFunction,
            functionSimpleName,
            functionParameters.map { getKDocSafeName(it) }
        )
    }

    fun generateSeeFunctionKDoc(
        nameOfClassContainingFunction: TypeName,
        funSpec: FunSpec
    ): CodeBlock {
        return generateSeeFunctionKDocInternal(
            nameOfClassContainingFunction,
            funSpec.name,
            funSpec.parameters.map { it.type }
        )
    }

    private fun getKDocSafeName(element: Element): TypeName {
        val typeUtils = processingEnvironment.typeUtils
        val type = element.asType()
        if (typeUtils.asElement(type) == null) {
            return type.asTypeName()
        }
        val simpleName = typeUtils.asElement(type).simpleName
        return ClassName.bestGuess(simpleName.toString())
    }

    private fun generateSeeFunctionKDocInternal(
        nameOfClassContainingFunction: TypeName,
        methodName: String,
        safeParameterNames: List<Any>
    ): CodeBlock {
        var kDocString = StringBuilder("@see %T#%L(")
        val kDocArgs = ArrayList<Any>()
        kDocArgs.add(nameOfClassContainingFunction)
        kDocArgs.add(methodName)
        for (param in safeParameterNames) {
            kDocString.append("%T, ")
            kDocArgs.add(param)
        }
        if (kDocArgs.size > 2) {
            kDocString = StringBuilder(kDocString.dropLast(2))
        }
        kDocString.append(")\n")
        return CodeBlock.of(kDocString.toString(), *(kDocArgs.toArray()))
    }

    fun generateCastingSuperCall(toReturn: TypeName, function: FunSpec): CodeBlock {
        return CodeBlock.builder()
            .add("return super.%N(", function.name)
            .add(function.parameters.map { it.name }.joinToString(separator = ","))
            .add(") as %T\n", toReturn)
            .build()
    }

    fun overriding(function: ExecutableElement): FunSpec.Builder {
        val functionName = function.simpleName.toString()
        val builder = FunSpec.builder(functionName)
            .addModifiers(KModifier.OVERRIDE)

        val defaultModifier = Modifier.valueOf("DEFAULT")
        val modifiers = function.modifiers.filter { it != Modifier.ABSTRACT && it != defaultModifier }
        builder.jvmModifiers(modifiers)
        return builder
            .addTypeVariables(function.typeParameters.map { it.asTypeVariableName().copy(nullable = false, bounds = emptyList()) })
            .returns(
                function.returnType.asTypeName().copy(nullable = !function.returnType.kind.isPrimitive &&
                        !function.annotationMirrors.any { it.annotationType.asElement().simpleName.toString() == "NonNull" }))
            .addParameters(getParameters(function))
    }

    fun getParameters(element: ExecutableElement): List<ParameterSpec> {
        return getParameters(element.parameters)
    }

    fun getParameters(parameters: List<out VariableElement>): List<ParameterSpec> {
        val result = parameters.map { getParameter(it) }
        return dedupedParameters(result)
    }

    fun dedupedParameters(parameters: List<ParameterSpec>): List<ParameterSpec> {
        var hasDupes = false

        val names = HashSet<String>()

        for (parameter in parameters) {
            val name = parameter.name
            if (names.contains(name)) {
                hasDupes = true
            } else {
                names.add(name)
            }
        }

        return if (hasDupes) {
            return parameters.mapIndexed { index, parameter->
                ParameterSpec
                    .builder("${parameter.name}$index", parameter.type)
                    .addModifiers(parameter.modifiers)
                    .addAnnotations(parameter.annotations)
                    .build()
            }
        } else {
            parameters
        }
    }

    fun getParameter(parameter: VariableElement): ParameterSpec {
        var type = parameter.asType().asTypeName()
        if (parameter.annotationMirrors.all { it.annotationType.asElement().simpleName.toString() != "NonNull" } && !parameter.asType().kind.isPrimitive) {
            type = type.copy(nullable = true)
        }

        return ParameterSpec.builder(computeParameterName(parameter, type), type)
            .jvmModifiers(parameter.modifiers)
            .addAnnotations(getAnnotations(parameter))
            .build()

    }

    private fun computeParameterName(parameter: VariableElement, type: TypeName): String {

        var rawClassName = ""
        if (type is ClassName) {
            rawClassName = type.simpleName
        }
        if (type is ParameterizedTypeName) {
            rawClassName = type.rawType.simpleName
        }

        rawClassName = applySmartParameterNameReplacements(rawClassName)
        val allCaps = rawClassName.toCharArray().all { it.isUpperCase() }
        if (rawClassName == "")
            return parameter.simpleName.toString()
        return if (allCaps) {
            rawClassName.toLowerCase()
        } else {
            rawClassName.split("(?=([A-Z])").last().toLowerCase()
        }
    }

    private fun getSmartPrimitiveParameterName(parameter: VariableElement): String {
        for (annotation in parameter.annotationMirrors) {
            val annotationName = annotation.annotationType.toString().toUpperCase()
            if (annotationName.endsWith("RES")) {
                return "id"
            } else if (annotationName.endsWith("RANGE")) {
                return "value"
            }
        }
        return parameter.simpleName.toString()
    }

    private fun applySmartParameterNameReplacements(name: String): String {
        return name.replace("[]", "s")
            .replace(Class::class.java.simpleName, "clazz")
            .replace(Object::class.java.simpleName, "o")
    }

    fun getAnnotations(element: Element) = element.annotationMirrors.map { AnnotationSpec.get(it) }

    fun visibleForTesting() = findAnnotationClassName(ANDROIDX_VISIBLE_FOR_TESTING, SUPPORT_VISIBLE_FOR_TESTING)

    fun nonNull(): ClassName {
        return findAnnotationClassName(ANDROIDX_NONNULL_ANNOTATION, SUPPORT_NONNULL_ANNOTATION)
    }

    fun checkResult(): ClassName {
        return findAnnotationClassName(ANDROIDX_CHECK_RESULT_ANNOTATION, SUPPORT_CHECK_RESULT_ANNOTATION)
    }

    fun nonNulls() = listOf(SUPPORT_NONNULL_ANNOTATION, JETBRAINS_NOTNULL_ANNOTATION, ANDROIDX_NONNULL_ANNOTATION)

    private fun findAnnotationClassName(androidxName: ClassName, supportName: ClassName): ClassName {
        val elements = processingEnvironment.elementUtils
        val visibleForTestingTypeElement = elements.getTypeElement(androidxName.reflectionName())
        return if (visibleForTestingTypeElement == null) supportName else androidxName
    }

}


