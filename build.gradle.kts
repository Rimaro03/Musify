// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    id("com.google.dagger.hilt.android") version "2.59.2" apply false
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.10" apply false
    id("androidx.navigation.safeargs.kotlin") version "2.9.7" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
}