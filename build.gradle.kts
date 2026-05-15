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
version = "3.9.7"
description = "ForceItemBattle for McPlayHD.net"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

repositories {
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
    maven {
        name = "mcplayhd"
        url = uri("https://maven.mcplayhd.net/public-releases")
    }
}

dependencies {
    paperweight.paperDevBundle("26.1.2.build.+")
    implementation("org.apache.commons:commons-text:1.13.1")
    implementation("de.threeseconds:FIBServiceClient:0.0.3")
    // paperweight.foliaDevBundle("1.20.4-R0.1-SNAPSHOT")
    // paperweight.devBundle("com.example.paperfork", "1.20.4-R0.1-SNAPSHOT")
}

tasks {
    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 25
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
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
commands.register("asktrade")
commands.register("trade")
commands.register("shout")
commands.register("fixskips")
commands.register("achievements")
commands.register("spectate")
commands.register("forceteam")
commands.register("vote")
commands.register("voteskip")
}