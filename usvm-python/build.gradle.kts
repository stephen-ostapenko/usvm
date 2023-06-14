plugins {
    id("usvm.kotlin-conventions")
}


dependencies {
    implementation(project(":usvm-core"))

    implementation("io.ksmt:ksmt-yices:${Versions.ksmt}")
    implementation("io.ksmt:ksmt-cvc5:${Versions.ksmt}")
    implementation("io.ksmt:ksmt-bitwuzla:${Versions.ksmt}")
    implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable-jvm:${Versions.collections}")
}

tasks.build {
    dependsOn(":usvm-python:cpythonadapter:linkDebug")
}

val cpythonBuildPath = "${childProjects["cpythonadapter"]!!.buildDir}/cpython_build"
val cpythonAdapterBuildPath = "${childProjects["cpythonadapter"]!!.buildDir}/lib/main/debug"

tasks.register<JavaExec>("runTestKt") {
    dependsOn(tasks.build)
    environment("LD_LIBRARY_PATH" to "$cpythonBuildPath/lib:$cpythonAdapterBuildPath")
    environment("LD_PRELOAD" to "$cpythonBuildPath/lib/libpython3.so")
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("TestKt")
}

tasks.register<JavaExec>("testRunner") {
    dependsOn(tasks.build)
    environment("LD_LIBRARY_PATH" to "$cpythonBuildPath/lib:$cpythonAdapterBuildPath")
    environment("LD_PRELOAD" to "$cpythonBuildPath/lib/libpython3.so")
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("org.usvm.samples.PythonTestRunnerKt")
}

sourceSets {
    val samples by creating {
        java {
            srcDir("src/samples/java")
        }
    }

    test {
        compileClasspath += samples.output
        runtimeClasspath += samples.output
    }
}