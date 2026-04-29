@file:OptIn(ExperimentalWasmDsl::class)

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

group = "org.findaname"

repositories {
    google()
    mavenCentral()
}

kotlin {
    compilerOptions {
        allWarningsAsErrors = true
    }
    wasmJs {
        browser {
            commonWebpackConfig {
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    port = 8081   // <-- porta desiderata per il frontend
                }
            }
        }
        binaries.executable()
    }

    sourceSets {
        commonMain {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(libs.voyager.navigation)

                implementation("io.ktor:ktor-client-core:3.0.3")
                implementation("io.ktor:ktor-client-content-negotiation:3.0.3")
                implementation("io.ktor:ktor-serialization-kotlinx-json:3.0.3")

                implementation("io.github.vinceglb:filekit-core:0.13.0")
                implementation("io.github.vinceglb:filekit-dialogs-compose:0.13.0")
                implementation("io.github.vinceglb:filekit-dialogs-compose:0.13.0")
            }
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
    }

    tasks.named("wasmJsBrowserTest") {
        enabled = false
    }

    targets.configureEach {
        compilations.configureEach {
            compileTaskProvider.configure {
                compilerOptions {
                    freeCompilerArgs.add("-Xexpect-actual-classes")
                }
            }
        }
    }
}
