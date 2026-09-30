plugins {
    alias(libs.plugins.navkit.android.application)
}

dependencies {
    implementation(projects.sample.shared)
    // The activity hands incoming links to `DeepLinkIngress`, a kit contract; nothing is re-exported,
    // so it names the contracts module itself.
    implementation(projects.navigation.api)
    // `setContent`: everything on screen is Compose Multiplatform, hosted in one activity.
    implementation(libs.androidx.activity.compose)
}
