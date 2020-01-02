import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.jvm.tasks.Jar

description = "Ksp - Symbol processing for Kotlin"

plugins {
    kotlin("jvm")
}

val packedJars by configurations.creating

dependencies {
    compile(kotlinStdlib())
    packedJars(project(":kotlin-symbol-processing")) { isTransitive = false }
    packedJars(project(":kotlin-symbol-processing-api")) { isTransitive = false }
    packedJars(project(":kotlin-ksp-gradle")) { isTransitive = false }
    runtime(projectRuntimeJar(":kotlin-compiler-embeddable"))
}

projectTest(parallel = true) {
    workingDir = projectDir
}

publish()

tasks.named<Jar>("jar").configure {
    classifier = "base"
}

runtimeJar(rewriteDepsToShadedCompiler(
    tasks.register<ShadowJar>("shadowJar") {
        from(packedJars)
    }
))

sourcesJar()
javadocJar()
