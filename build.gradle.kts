plugins {
    id("java")
    id("checkstyle")
    id("com.github.spotbugs") version "6.0.14"
    id("io.freefair.lombok") version ("6.6.1")
}

group = "com.aienginerestapi"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven {
        url = uri("https://personal-472156860409.d.codeartifact.eu-north-1.amazonaws.com/maven/AIChat/")
        credentials {
            username = "aws"
            password = findProperty("awsCodeArtifactToken") as String?
                ?: System.getenv("CODEARTIFACT_AUTH_TOKEN")
        }
    }
}

dependencies {
    testImplementation("org.testng:testng:7.8.0")

    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")

    implementation("com.fasterxml.jackson.core:jackson-databind:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-core:2.17.0")
    implementation("com.fasterxml.jackson.core:jackson-annotations:2.17.0")

    implementation("com.aienginerestapi:AIEngineRestApiModel:2.0.3")

    implementation(platform("software.amazon.awssdk:bom:2.25.8"))
    implementation("software.amazon.awssdk:regions")
    implementation("software.amazon.awssdk:ssm")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

tasks {
    test {
        useTestNG()
    }

    withType<Checkstyle>().configureEach {
        configFile = file("config/checkstyle/checkstyle.xml")
        configProperties = mapOf(
            "checkstyle.suppressions.file" to file("config/checkstyle/suppressions.xml").absolutePath
        )
    }

    named<com.github.spotbugs.snom.SpotBugsTask>("spotbugsMain") {
        reports {
            create("html") {
                required.set(true)
                outputLocation.set(
                    layout.buildDirectory.file("reports/spotbugs/main.html")
                )
            }
        }
        effort.set(com.github.spotbugs.snom.Effort.MAX)
        reportLevel.set(com.github.spotbugs.snom.Confidence.LOW)
    }

    named("build") {
        dependsOn(
            "spotbugsMain",
        )
    }
}
