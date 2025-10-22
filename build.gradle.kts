plugins {
    java
}

group = "com.example"
version = "1.0.0"

description = "Standalone player shop block plugin"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
}

tasks.jar {
    archiveBaseName.set("ShopBlock")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
