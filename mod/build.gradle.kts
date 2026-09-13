plugins {
    id("fabric-loom") version "1.17.12"
    java
}

version = project.property("mod_version") as String
group = project.property("maven_group") as String

base {
    archivesName.set(project.property("archives_base_name") as String)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") { name = "Fabric" }
    // GeckoLib publishes nowhere else. The group filter keeps every other
    // resolution from paying for a round trip to Cloudsmith.
    maven("https://dl.cloudsmith.io/public/geckolib3/geckolib/maven/") {
        name = "GeckoLib"
        content { includeGroup("software.bernie.geckolib") }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${project.property("minecraft_version")}")
    mappings(loom.officialMojangMappings())
    modImplementation("net.fabricmc:fabric-loader:${project.property("loader_version")}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${project.property("fabric_version")}")

    // Not `include`d: GeckoLib is a mod a player installs, and bundling a copy
    // would fight the one already in their folder.
    modImplementation("software.bernie.geckolib:geckolib-fabric-${project.property("minecraft_version")}:${project.property("geckolib_version")}")

    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(21)
    // Mojang marks the methods a mod is not meant to call, so the warning has to name them rather
    // than sit collapsed behind "recompile with".
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}

tasks.test {
    useJUnitPlatform()
}

loom {
    runs {
        named("client") {
            runDirectory.set(layout.projectDirectory.dir("run"))
            jvmArguments.addAll("-Xms2G", "-Xmx4G", "-XX:+UseG1GC")
            // The client refuses to start with two quick-play options, so this has to replace the
            // one the launch would otherwise carry rather than add to it. Values: "none",
            // "solo:<world>", "join:<host:port>". A headless session uses the last one, which is
            // what puts the client inside the world the server console is driving.
            val quickPlay = (project.findProperty("tdQuickPlay") as String?) ?: "none"
            if (quickPlay.startsWith("join:")) {
                programArguments.addAll("--quickPlayMultiplayer", quickPlay.removePrefix("join:"))
            } else if (quickPlay.startsWith("solo:")) {
                programArguments.addAll("--quickPlaySingleplayer", quickPlay.removePrefix("solo:"))
            }
        }
        named("server") {
            runDirectory.set(layout.projectDirectory.dir("run/server"))
            jvmArguments.addAll("-Xms1G", "-Xmx3G", "-XX:+UseG1GC")
        }
    }
}

tasks.jar {
    from("../LICENSE")
}
