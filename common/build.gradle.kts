plugins {
    id("maven-publish")
    id("net.neoforged.moddev")
}

base {
    archivesName = "${property("mod_id")}-${project.name}-${property("minecraft_version")}"
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(property("java_version").toString())
    }
    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
    exclusiveContent {
        forRepository {
            maven {
                name = "Sponge"
                url = uri("https://repo.spongepowered.org/repository/maven-public")
            }
        }
        filter {
            includeGroupAndSubgroups("org.spongepowered")
        }
    }
}

tasks.named<Jar>("sourcesJar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${findProperty("mod_name").toString()}" }
    }
}
tasks.named<Jar>("jar") {
    from(rootProject.file("LICENSE")) {
        rename { "${it}_${findProperty("mod_name").toString()}" }
    }
    manifest {
        attributes(
            "Specification-Title"    to findProperty("mod_name").toString(),
            "Specification-Vendor"   to findProperty("mod_author").toString(),
            "Specification-Version"  to findProperty("mod_version").toString(),
            "Implementation-Title"   to findProperty("mod_id").toString(),
            "Implementation-Version" to findProperty("mod_version").toString(),
            "Implementation-Vendor"  to findProperty("mod_author").toString(),
            "Built-On-Minecraft"     to findProperty("minecraft_version").toString(),
         )
    }
}

tasks.named<ProcessResources>("processResources") {
    val replaceProperties = mapOf(
        "mod_version"                   to findProperty("mod_version"),
        "mod_group_id"                  to findProperty("mod_group_id"),
        "minecraft_version"             to findProperty("minecraft_version"),
        "minecraft_version_range"       to findProperty("minecraft_version_range"),
        "fabric_version"                to findProperty("fabric_version"),
        "fabric_loader_version"         to findProperty("fabric_loader_version"),
        "mod_name"                      to findProperty("mod_name"),
        "mod_author"                    to findProperty("mod_author"),
        "mod_id"                        to findProperty("mod_id"),
        "mod_logo_file"                 to findProperty("mod_logo_file"),
        "mod_license"                   to findProperty("mod_license"),
        "mod_description"               to findProperty("mod_description"),
        "neoforge_version"              to findProperty("neoforge_version"),
        "neoforge_version_range"        to findProperty("neoforge_version_range"),
        "neoforge_loader_version_range" to findProperty("neoforge_loader_version_range"),
        "mod_credits"                   to findProperty("mod_credits"),
        "java_version"                  to findProperty("java_version"),
    )
    inputs.properties(replaceProperties)
    expand(replaceProperties)
    from("src/main/templates")
}

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

neoForge {
    neoFormVersion = "${property("neo_form_version")}"
    val at = file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }
}

dependencies {
    compileOnly("org.spongepowered:mixin:0.8.5")
}

configurations {
    create("commonJava")
    create("commonResources")
}

artifacts {
    add("commonJava",      sourceSets.main.get().java.srcDirs.first { it.name == "java" })
    add("commonResources", sourceSets.main.get().resources.srcDirs.first { it.name == "resources" })
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            artifactId = base.archivesName.get()
            from(components["java"])
        }
    }
    repositories {
        maven {
            url = uri(
                System.getenv("local_maven_url")
                    ?: layout.buildDirectory.dir("repo").get().asFile.toURI().toString()
            )
        }
    }
}
