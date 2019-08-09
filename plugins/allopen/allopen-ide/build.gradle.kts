
description = "Kotlin AllOpen IDEA Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    google()
    jcenter()
}

dependencies {
    compile(project(":kotlin-allopen-compiler-plugin"))
    compile(project(":compiler:util"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:cli-common"))
    compile(project(":idea"))
    compile(project(":idea:idea-jvm"))
    compile(project(":idea:idea-jps-common"))
    compile("com.squareup:kotlinpoet:1.3.0")
    compile(project(":plugins:annotation-based-compiler-plugins-ide-support"))
    compileOnly(intellijDep())
    excludeInAndroidStudio(rootProject) { compileOnly(intellijPluginDep("maven")) }
    compileOnly(intellijPluginDep("gradle"))
}

sourceSets {
    "main" { projectDefault() }
    "test" {}
}

runtimeJar()
