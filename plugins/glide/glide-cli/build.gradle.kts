
description = "Kotlin Glide Compiler Plugin"

plugins {
    kotlin("jvm")
    id("jps-compatible")
}

repositories {
    flatDir {
        dirs("lib")
    }
    jcenter()
    google()
}

dependencies {
    testCompileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    testRuntime(intellijDep())
    testCompileOnly(intellijDep()) { includeJars("idea", "idea_rt", "openapi") }

    Platform[181].orHigher {
        testCompileOnly(intellijDep()) { includeJars("platform-api", "platform-impl") }
    }

    compile(project(":core:util.runtime"))
    compile(project(":compiler:util"))
    compile(project(":compiler:cli"))
    compile(project(":compiler:backend"))
    compile(project(":compiler:frontend"))
    compile(project(":compiler:frontend.java"))
    compile(project(":compiler:plugin-api"))
    compile("com.android.support:support-annotations:+")
    compile("com.android.support:support-fragment:28.0.0")
    compile("com.android.tools.build:gradle:3.4.2")
    compile("com.github.bumptech.glide:compiler:4.9.0")
    compile("com.squareup:kotlinpoet:1.3.0")
    compile(commonDep("com.google.android", "android"))
    compile(files("lib/glide-4.9.0.jar"))
    compileOnly(project(":kotlin-annotation-processing-cli"))
    compileOnly(project(":kotlin-annotation-processing-base"))
    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

    testCompile(projectTests(":compiler:tests-common"))
    testCompile(project(":kotlin-annotation-processing-base"))
    testCompile(projectTests(":kotlin-annotation-processing-base"))
    testCompile(commonDep("junit:junit"))
    testCompile(project(":kotlin-annotation-processing-runtime"))

    embedded(project(":kotlin-annotation-processing-runtime")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-cli")) { isTransitive = false }
    embedded(project(":kotlin-annotation-processing-base")) { isTransitive = false }

    api(project(":plugins:uast-kotlin"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

testsJar {}

projectTest(parallel = true) {
    workingDir = rootDir
    dependsOn(":dist")
}

publish()

runtimeJar()

sourcesJar()
javadocJar()
