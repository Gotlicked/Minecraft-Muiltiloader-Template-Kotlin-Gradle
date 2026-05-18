plugins {
    id("net.neoforged.moddev")
    id("java-library")
    id("maven-publish")
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
}

val commonJava: Configuration by configurations.creating
val commonResources: Configuration by configurations.creating

sourceSets.main {
    resources.srcDir("src/generated/resources")
}

neoForge {
    version = "${property("neoforge_version")}"

    val at = project(":common").file("src/main/resources/META-INF/accesstransformer.cfg")
    if (at.exists()) {
        accessTransformers.from(at.absolutePath)
    }

    runs {
        create("client") {
            client()
            ideName = "NeoForge Client"
            gameDirectory = mkdir(file("runs/client"))
        }
        create("data") {
            clientData()
            ideName = "NeoForge Data"
            gameDirectory = mkdir(file("runs/data"))
            programArguments.addAll(
                "--mod", "${property("mod_id")}", "--all", "--output",
                file("src/generated/resources/").absolutePath,
                "--existing", file("src/main/resources/").absolutePath,
            )
        }
        create("server") {
            server()
            ideName = "NeoForge Server"
            gameDirectory = mkdir(file("runs/server"))
        }
    }

    mods {
        create("${property("mod_id")}") {
            sourceSet(sourceSets.main.get())
        }
    }
}

dependencies {
    compileOnly(project(":common"))
    add("commonJava",      project(path = ":common", configuration = "commonJava"))
    add("commonResources", project(path = ":common", configuration = "commonResources"))
}

tasks.named<JavaCompile>("compileJava") {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(commonResources)
    from(commonResources)
    expand(project.properties)
    from("src/main/templates")
}

tasks.named<Javadoc>("javadoc") {
    dependsOn(commonJava)
    source(commonJava)
}

tasks.named<Jar>("sourcesJar") {
    dependsOn(commonJava, commonResources)
    from(commonJava)
    from(commonResources)
}

tasks.named<Jar>("jar") {
    dependsOn(commonJava, commonResources)
    from(commonJava)
    from(commonResources)
}

tasks.withType<Copy>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
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
