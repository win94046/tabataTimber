import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.sqldelight)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = true
        }
    }
    
    sourceSets {
        commonMain.dependencies {
            // Koin for Dependency Injection
            implementation(libs.koin.core)
            // DateTime library
            implementation(libs.kotlinx.datetime)
            // SQLDelight runtime
            implementation(libs.sqldelight.runtime)
        }
        
        androidMain.dependencies {
            // SQLDelight Android driver
            implementation(libs.sqldelight.android.driver)
        }
        
        nativeMain.dependencies {
            // SQLDelight Native driver (iOS)
            implementation(libs.sqldelight.native.driver)
        }
        
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.koin.test)
            // SQLDelight SQLite driver for testing
            implementation(libs.sqldelight.sqlite.driver)
        }
    }
}

sqldelight {
    databases {
        create("TabataTimberDatabase") {
            packageName.set("org.tabata.timber.database")
            srcDirs.setFrom("src/commonMain/sqldelight")
            schemaOutputDirectory.set(file("src/commonMain/sqldelight/databases"))
            deriveSchemaFromMigrations.set(true)
            verifyMigrations.set(true)
        }
    }
}

android {
    namespace = "org.tabata.timber.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
