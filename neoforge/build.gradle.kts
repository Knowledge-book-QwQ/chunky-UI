plugins {
    id 'java-library'
    id 'net.neoforged.gradle.userdev' version '7.0.145'
}

group = "${mod_group}.neoforge"
version = mod_version

base {
    archivesName = "${mod_id}-neoforge-${minecraft_version}"
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    mavenCentral()
    // Chunky通过CurseForge/Modrinth分发，运行时由玩家安装
    // 编译时仅通过反射访问ChunkyAPI，不直接依赖
}

dependencies {
    implementation "net.neoforged:neoforge:${neoforge_version}"

    // 依赖common模块
    implementation project(':common')
}

// NeoForge运行配置
runs {
    configureEach {
        systemProperty 'forge.logging.markers', 'REGISTRIES'
        systemProperty 'forge.logging.console.level', 'debug'
        modSource project.sourceSets.main
    }

    client {
        systemProperty 'neoforge.enabledGameTestNamespaces', mod_id
    }

    server {
        systemProperty 'neoforge.enabledGameTestNamespaces', mod_id
        programArgument '--nogui'
    }
}

// 模组资源处理
sourceSets.main.resources { srcDir 'src/generated/resources' }
