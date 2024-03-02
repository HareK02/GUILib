import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

group = "net.hareworks"

version = "1.0"

plugins {
  kotlin("jvm") version "1.9.22"
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
  id("com.github.johnrengelman.shadow") version "8.1.1"
  id("io.papermc.paperweight.userdev") version "1.5.10"
}

repositories {
  mavenCentral()
  maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
  compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
  compileOnly("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
  paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")
}

tasks {
  shadowJar {
    archiveBaseName.set("GuiLib")
    archiveClassifier.set("")
  }
}

bukkit {
  main = "net.hareworks.guilib.GuiLib"
  name = "GuiLib"
  description = "GuiLib"
  version = getVersion().toString()
  apiVersion = "1.20"
  authors =
      listOf(
          "Hare-K02",
      )
}