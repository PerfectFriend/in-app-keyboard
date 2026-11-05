plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
}

android {
  namespace = "com.narde.keyboard"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    minSdk = 24
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = "2024.09.00"
  }
}

dependencies {
  implementation(platform("androidx.compose:compose-bom:2024.09.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-text")
  implementation("androidx.compose.runtime:runtime")
  implementation("androidx.compose.foundation:foundation")
  implementation("androidx.compose.material3:material3")
  implementation("androidx.core:core-ktx:1.18.0")

  testImplementation("junit:junit:4.13.2")
}
