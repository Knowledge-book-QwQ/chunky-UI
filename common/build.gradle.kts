plugins {
    id 'java-library'
}

group = "${mod_group}.common"
version = mod_version

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withSourcesJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // 纯Java模块，不依赖Minecraft
    // 仅使用标准库 + 日志
    compileOnly 'org.jetbrains:annotations:24.1.0'

    testImplementation 'org.junit.jupiter:junit-jupiter:5.11.0'
    testImplementation 'org.assertj:assertj-core:3.26.3'
}

test {
    useJUnitPlatform()
}
