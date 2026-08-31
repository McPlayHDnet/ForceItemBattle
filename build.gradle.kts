import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.21"
    id("xyz.jpenilla.run-paper") version "3.0.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.1" // Generates plugin.yml based on the Gradle config
    id("io.freefair.lombok") version "9.0.0"
    id("com.gradleup.shadow") version "9.4.1"
}

group = "forceitembattle"
version = "26.8.5" // year.month.update
description = "ForceItemBattle for McPlayHD.net"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    maven {
        name = "PaperMC"
        url = uri("https://repo.papermc.io/repository/maven-public/")
    }
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        /**
         * url = uri("https://maven.mcplayhd.net/public-releases")
         * url = uri("https://maven.mcplayhd.net/releases")
         * credentials(PasswordCredentials::class)
         */
        name = "mcplayhd"
        url = uri("https://maven.mcplayhd.net/public-releases")
        // credentials(PasswordCredentials::class)
    }
}

/**
 * Keep the paperweight server artifact off the test classpath.
 *
 * MockBukkit and paperweight both provide a server implementation, and with both present the real
 * Paper classes win -- booting MockBukkit then dies on
 * "RegistryKeyImpl[key=minecraft:attribute] points to a registry that is not available yet",
 * which is the same attribute registry HeadlessBoundaryTest has been pinning since pass 1.
 *
 * This limits the server dependency to compileOnly, which is where a plugin wants it anyway. The
 * documented cost is that NMS behaviour is unavailable in tests; this plugin imports no
 * net.minecraft or craftbukkit types at all, so it costs nothing here. If that ever changes, those
 * paths stay the harness's job rather than the unit suite's.
 */
paperweight {
    addServerDependencyTo = configurations.named(JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME).map { setOf(it) }
}

dependencies {
    paperweight.paperDevBundle("26.2.build.+")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("de.threeseconds:FIBServiceClient:1.0.3")
    // paperweight.foliaDevBundle("1.20.4-R0.1-SNAPSHOT")
    // paperweight.devBundle("com.example.paperfork", "1.20.4-R0.1-SNAPSHOT")

    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockbukkit.mockbukkit:mockbukkit-v26.2:4.116.1")
    // MockBukkit's POM declares no Paper API -- the consumer supplies it. paperweight now puts the
    // server artifact on compileOnly only (see the paperweight block above), so tests need this.
    testImplementation("io.papermc.paper:paper-api:26.2.build.111-stable")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 25
        options.compilerArgs.add("-Xlint:all")
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    test {
        useJUnitPlatform()

        // A fresh JVM per test class.
        forkEvery = 1

        // Byte Buddy, which Mockito uses to mock Bukkit interfaces, does not yet recognise Java 25,
        // so it needs this to instrument the Player type hierarchy.
        systemProperty("net.bytebuddy.experimental", "true")

        // Mockito's inline mock maker self-attaches as an agent by default and warns that this will
        // stop working. Attaching it explicitly from the test classpath silences that.
        doFirst {
            classpath.files
                .firstOrNull { it.name.startsWith("mockito-core") }
                ?.let { jvmArgs("-javaagent:${it.absolutePath}") }
        }

        testLogging {
            events("failed")
            showStandardStreams = false
        }
    }

    shadowJar {
        relocate("org.openapitools", "forceitembattle.libs.openapitools")
        relocate("okhttp3", "forceitembattle.libs.okhttp3")
        relocate("okio", "forceitembattle.libs.okio")
        relocate("io.gsonfire", "forceitembattle.libs.gsonfire")

        minimize {
            exclude(dependency("de.threeseconds:FIBServiceClient:.*"))
        }

        archiveClassifier.set("")
    }

    build {
        dependsOn(shadowJar)
    }
}

// Configure plugin.yml generation
// - name, version, and description are inherited from the Gradle project.
bukkitPluginYaml {
    main = "forceitembattle.ForceItemBattle"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.add("threeseconds")
    authors.add("stupxd")
    authors.add("eltobito")
    apiVersion = "26.1.2"
    commands.register("start")
    commands.register("settings")
    commands.register("skip")
    commands.register("reset")
    commands.register("bp")
    commands.register("result")
    commands.register("items")
    commands.register("info")
    commands.register("infowiki")
    commands.register("spawn")
    commands.register("bed")
    commands.register("pause")
    commands.register("resume")
    commands.register("help")
    commands.register("stats")
    commands.register("top")
    commands.register("pos")
    commands.register("ping")
    commands.register("stoptimer")
    commands.register("teams")
    commands.register("shout")
    commands.register("fixskips")
    commands.register("achievements")
    commands.register("spectate")
    commands.register("forceteam")
    commands.register("vote")
    commands.register("voteskip")
    commands.register("fixlocate")
    commands.register("forceitem")
    commands.register("randomevent")
    commands.register("collection")
}
