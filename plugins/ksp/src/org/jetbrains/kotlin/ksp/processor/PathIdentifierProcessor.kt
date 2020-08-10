/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ksp.processor

import org.jetbrains.kotlin.ksp.processing.Resolver
import org.jetbrains.kotlin.ksp.symbol.*
import org.jetbrains.kotlin.ksp.visitor.KSTopDownVisitor

class PathIdentifierProcessor : AbstractTestProcessor() {
    val result = mutableListOf<String>()

    override fun toResult(): List<String> {
        return result
    }

    override fun process(resolver: Resolver) {
        val visitor = NameCollector()
        resolver.getAllFiles().map { it.accept(visitor, result) }
    }

}

class NameCollector : KSTopDownVisitor<MutableCollection<String>, Unit>() {

    override fun defaultHandler(node: KSNode, data: MutableCollection<String>) {}

    override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: MutableCollection<String>) {
        classDeclaration.qualifiedName?.asPathIdentifier()?.let { data.add(if (it == "") "<no name>" else it) }
        super.visitClassDeclaration(classDeclaration, data)
    }

    override fun visitFunctionDeclaration(function: KSFunctionDeclaration, data: MutableCollection<String>) {
        function.qualifiedName?.asPathIdentifier()?.let { data.add(if (it == "") "<no name>" else it) }
        super.visitFunctionDeclaration(function, data)
    }

    override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: MutableCollection<String>) {
        property.qualifiedName?.asPathIdentifier()?.let { data.add(if (it == "") "<no name>" else it) }
        super.visitPropertyDeclaration(property, data)
    }
}