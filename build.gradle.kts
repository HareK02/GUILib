import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

group = "net.hareworks"
version = "1.0"

plugins {
    kotlin("jvm") version "1.9.22"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}
repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
		maven ("https://repo.dmulloy2.net/repository/public/")
}
dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
		compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
}
tasks {
    shadowJar {
        archiveBaseName.set("SimplyTextDisplay")
        archiveClassifier.set("")
    }
}

bukkit {
    main = "net.hareworks.simplytextdisplay.SimplyTextDisplay"
    name = "SimplyTextDisplay"
    description = "TextDisplay with PlaceholderAPI"
    version = getVersion().toString()
		apiVersion = "1.20"
    authors =
            listOf(
                "Hare-K02",
            )
		depend = listOf("ProtocolLib")
    commands {
        register("textdisplay") {
            description = "textdisplay command"
            usage = "/textdisplay <subcommand>"
            permission = "textdisplay.command"
            permissionMessage = "§cYou don't have permission to use this command."
        }
		}
    permissions {
        register("textdisplay.command") {
            description = "textdisplay command"
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}