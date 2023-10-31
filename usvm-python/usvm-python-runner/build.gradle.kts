plugins {
    id("usvm.kotlin-conventions")
    `maven-publish`
}

dependencies {
    api("io.github.microutils:kotlin-logging:${Versions.klogging}")
    testImplementation("ch.qos.logback:logback-classic:${Versions.logback}")
}

tasks.register<JavaExec>("manualTestOfRunner") {
    group = "run"
    classpath = sourceSets.test.get().runtimeClasspath
    jvmArgs = listOf("-Dproject.root=${projectDir.parent}")
    mainClass.set("org.usvm.runner.ManualTestKt")
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/UnitTestBot/usvm")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}