// ChunkPregeneratorUI — 多平台MC区块预生成UI模组
// NeoForge 1.21.1 (优先) | Forge 1.20.1 | Fabric 1.21.1

plugins {
    id("java")
    id("idea")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

allprojects {
    group = "com.chunkpregenerator"
    version = "1.0.0"

    repositories {
        mavenCentral()
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.fabricmc.net")
        maven("https://libraries.minecraft.net")
    }
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "idea")

    java {
        toolchain.languageVersion = JavaLanguageVersion.of(21)
        withJavadocJar()
        withSourcesJar()
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.addAll(listOf("-Xlint:unchecked", "-Xlint:deprecation"))
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
