plugins {
    java
    alias(libs.plugins.shadow)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    enabled = false
}

tasks.shadowJar {
    archiveFileName.set("RePairip.jar")
    manifest {
        attributes["Main-Class"] = "com.antik.Main"
    }
}

// Make the 'build' and 'assemble' tasks produce the shadow jar
tasks.assemble {
    dependsOn(tasks.shadowJar)
}

dependencies {
    implementation(libs.dexlib2)
    implementation(libs.reandroid.arsclib)
    implementation(libs.guava)
}
