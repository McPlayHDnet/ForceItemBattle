import xyz.jpenilla.resourcefactory.bukkit.BukkitPluginYaml

plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.17"
    id("xyz.jpenilla.run-paper") version "2.3.1" // Adds runServer and runMojangMappedServer tasks for testing
    id("xyz.jpenilla.resource-factory-bukkit-convention") version "1.3.0" // Generates plugin.yml based on the Gradle config
    id("io.freefair.lombok") version "8.14"
}

group = "forceitembattle"
version = "3.7.0"
description = "ForceItemBattle for McPlayHD.net"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

repositories {
    maven {
        name = "CodeMC"
        url = uri("https://repo.codemc.io/repository/maven-public/")
    }
}

dependencies {
    paperweight.paperDevBundle("1.21.7-R0.1-SNAPSHOT")
    implementation("org.apache.commons:commons-text:1.13.1")
    // paperweight.foliaDevBundle("1.20.4-R0.1-SNAPSHOT")
    // paperweight.devBundle("com.example.paperfork", "1.20.4-R0.1-SNAPSHOT")
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release = 21
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar = layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar")
    }
     */
}

// Configure plugin.yml generation
// - name, version, and description are inherited from the Gradle project.
bukkitPluginYaml {
    main = "forceitembattle.ForceItemBattle"
    load = BukkitPluginYaml.PluginLoadOrder.STARTUP
    authors.add("threeseconds")
    authors.add("stupxd")
    authors.add("FireBladeHunter")
    apiVersion = "1.21"
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
