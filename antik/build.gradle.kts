import proguard.gradle.ProGuardTask

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
    destinationDirectory.set(layout.buildDirectory.dir("tmp"))
    archiveFileName.set("raw.jar")
    exclude("frameworks/**")

    manifest {
        attributes["Main-Class"] = "com.repairip.Main"
    }
    finalizedBy(proguard)
}

val rawJarFile = layout.buildDirectory.file("tmp/raw.jar")

val proguard = tasks.register<ProGuardTask>("proguard") {
    dependsOn(tasks.shadowJar)

    injars(rawJarFile)
    outjars(layout.buildDirectory.file("libs/RePairip.jar"))

    libraryjars("${System.getProperty("java.home")}/jmods")
    configuration("r8-rules.pro")

    val fileToDelete = rawJarFile.get().asFile
    doLast {
        fileToDelete.delete()
    }
}

tasks.assemble {
    dependsOn(proguard)
}

dependencies {
    implementation(libs.dexlib2)
    implementation(libs.reandroid.arsclib)
    implementation(libs.guava)
}
