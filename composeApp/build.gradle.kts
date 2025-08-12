import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.internal.extensions.stdlib.capitalized
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

val APP_NAME = "Kreate"

plugins {
    // Multiplatform
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.jetbrains.compose)

    // Android
    alias(libs.plugins.android.application)
    alias(libs.plugins.room)


    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.kotlin.serialization)
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
            freeCompilerArgs.add("-Xcontext-parameters")
        }
    }

    jvm("desktop")



    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val desktopMain by getting
        desktopMain.dependencies {
            implementation(compose.components.resources)
            implementation(compose.desktop.currentOs)

            implementation(libs.material.icon.desktop)
            implementation(libs.vlcj)

            runtimeOnly(libs.kotlinx.coroutines.swing)

            /*
            // Uncomment only for build jvm desktop version
            // Comment before build android version
            configurations.commonMainApi {
                exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-android")
            }
            */
        }

        androidMain.dependencies {
            implementation(libs.media3.session)
            implementation(libs.kotlinx.coroutines.guava)
            implementation(libs.newpipe.extractor)
            implementation(libs.nanojson)
            implementation(libs.androidx.webkit)

            // Related to built-in game, maybe removed in future?
            implementation(libs.compose.runtime.livedata)

            implementation(libs.ktor.core)
            implementation(libs.ktor.okhttp)
            implementation(libs.ktor.content.negotiation)
            implementation(libs.ktor.serialization.protobuf)
            implementation(libs.ktor.encoding)
            implementation( libs.ktor.serialization.json )
            implementation(libs.okhttp3.logging.interceptor)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)

            implementation(projects.innertube)
            implementation(projects.oldtube)

            implementation(libs.room)
            implementation(libs.room.runtime)
            implementation(libs.room.sqlite.bundled)

            implementation(libs.navigation.kmp)

            //coil3 mp
            implementation(libs.coil3.compose.core)
            implementation( libs.coil3.network.ktor )

            implementation(libs.translator)

            implementation( libs.bundles.compose.kmp )
        }
    }
}

android {
    dependenciesInfo {
        // Disables dependency metadata when building APKs.
        includeInApk = false
        // Disables dependency metadata when building Android App Bundles.
        includeInBundle = false
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    compileSdk = 36

    defaultConfig {
        applicationId = "me.knighthat.kreate"
        minSdk = 21
        targetSdk = 36
        versionCode = 111
        versionName = "1.6.4"

        /*
                UNIVERSAL VARIABLES
         */
        buildConfigField( "String", "APP_NAME", "\"$APP_NAME\"" )
    }

    splits {
        abi {
            reset()
            isUniversalApk = true
        }
    }

    namespace = "app.kreate.android"

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["appName"] = "$APP_NAME-debug"
        }

        // To test compatibility after minification process
        create( "debugR8" ) {
            initWith( maybeCreate( "debug" ) )

            // Package optimization
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }

        release {
            isDefault = true

            // Package optimization
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create( "uncompressed" ) {
            // App's properties
            versionNameSuffix = "-f"
        }

        /**
         * For convenience only.
         * "Forkers" want to change app name across builds
         * just need to change this variable
         */
        forEach {
            it.manifestPlaceholders.putIfAbsent( "appName", APP_NAME )
        }
    }

    flavorDimensions += listOf( "prod" )
    productFlavors {
        create("github") {
            dimension = "prod"

            isDefault = true
        }

        create( "fdroid" ) {
            dimension = "prod"

            // App's properties
            versionNameSuffix = "-fdroid"
        }

        create( "izzy" ) {
            dimension = "prod"

            // App's properties
            versionNameSuffix = "-izzy"
        }
    }

    applicationVariants.all {
        outputs.map { it as BaseVariantOutputImpl }
               .forEach {
                   val suffix = if( flavorName == "izzy" ) "izzy" else buildType.name
                   it.outputFileName = "$APP_NAME-${suffix}.apk"
               }

        if( buildType.name != "debug" ) {
            val capitalizedFlavorName = "${flavorName.capitalized()}${buildType.name.capitalized()}"

            tasks.register<Copy>("copyReleaseNoteTo${capitalizedFlavorName}Res" ) {
                from( "$rootDir/fastlane/metadata/android/en-US/changelogs" )
                val fileName = "${android.defaultConfig.versionCode!!}.txt"
                setIncludes( listOf( fileName ) )

                into( "$rootDir/composeApp/src/android$capitalizedFlavorName/res/raw" )

                rename {
                    if( it == fileName ) "release_notes.txt" else it
                }
            }

            preBuildProvider.get().dependsOn( "copyReleaseNoteTo${capitalizedFlavorName}Res" )
        }
    }

    sourceSets.all {
        kotlin.srcDir("src/$name/kotlin")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

compose.desktop {
    application {

        mainClass = "MainKt"

        //conveyor
        version = "0.0.1"
        group = "rimusic"

        //jpackage
        nativeDistributions {
            //conveyor
            vendor = "RiMusic.DesktopApp"
            description = "RiMusic Desktop Music Player"

            targetFormats(TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = "RiMusic.DesktopApp"
            packageVersion = "0.0.1"
        }
    }
}

compose.resources {
    publicResClass = true
    generateResClass = always
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(libs.androidx.palette)
    implementation(libs.media3.exoplayer)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.appcompat.resources)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlin.csv)
    implementation(libs.monetcompat)
    implementation(libs.androidmaterial)
    implementation(libs.timber)
    implementation(libs.androidx.crypto)
    implementation(libs.math3)
    implementation(libs.toasty)
    implementation(libs.androidyoutubeplayer)
    implementation(libs.androidx.glance.widgets)
    implementation(libs.kizzy.rpc)
    implementation (libs.hypnoticcanvas)
    implementation (libs.hypnoticcanvas.shaders)

    implementation(libs.room)
    ksp(libs.room.compiler)

    implementation(projects.innertube)
    implementation(projects.oldtube)
    implementation(projects.kugou)
    implementation(projects.lrclib)

    coreLibraryDesugaring(libs.desugaring.nio)
}
