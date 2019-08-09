
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
    compile(commonDep("com.google.android", "android"))

    compile("com.github.bumptech.glide:compiler:4.9.0")
    compile("com.squareup:kotlinpoet:1.3.0")
//    compile("com.github.bumptech.glide:glide:4.9.0")

    compile(files("lib/glide-4.9.0.jar"))
//    compileOnly(project(":kotlin-annotation-processing-cli"))
//    compileOnly(project(":kotlin-annotation-processing-base"))
//    compileOnly(project(":kotlin-annotation-processing-runtime"))
    compileOnly(intellijCoreDep()) { includeJars("intellij-core") }
    compileOnly(intellijDep()) { includeJars("asm-all", rootProject = rootProject) }

//    embedded("com.android.support:support-annotations:+")
//    embedded("com.android.support:support-fragment:28.0.0")
//    embedded("com.android.tools.build:gradle:3.4.2")
    embedded("com.github.bumptech.glide:compiler:4.9.0")
//    embedded("com.github.bumptech.glide:glide:4.9.0")
    embedded("com.squareup:kotlinpoet:1.3.0")

    embedded(files("lib/glide-4.9.0.jar"))
    testCompile(projectTests(":compiler:tests-common"))
//    testCompile(project(":kotlin-annotation-processing-base"))
//    testCompile(projectTests(":kotlin-annotation-processing-base"))
    testCompile(commonDep("junit:junit"))
//    testCompile(project(":kotlin-annotation-processing-runtime"))
}

sourceSets {
    "main" { projectDefault() }
    "test" { projectDefault() }
}

runtimeJar()

testsJar()

projectTest(parallel = true) {
    workingDir = rootDir
}
