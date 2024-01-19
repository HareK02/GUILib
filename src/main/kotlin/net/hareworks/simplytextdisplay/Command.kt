package net.hareworks.simplytextdisplay

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

class Command : CommandExecutor {

  override fun onCommand(
      sender: CommandSender,
      command: Command,
      label: String,
      args: Array<String>
  ): Boolean {
    if (args.size == 0) {
      sender.sendMessage("§cUsage: /textdisplay <create|delete|list|modify>")
      return true
    }
    when (args[0]) {
      "create" -> {
        if (args.size < 2) {
          sender.sendMessage("§cUsage: /textdisplay create <text>")
          return true
        }
        val text = args.slice(1..args.size).joinToString(" ")
        sender.sendMessage(
            "§aCreated text display with id ${SimplyTextDisplay.plugin.textDisplays.size}"
        )
        SimplyTextDisplay.plugin.send(
            TextDisplay(
                    SimplyTextDisplay.plugin.textDisplays.size,
                    text,
                    (sender as Player).getLocation(),
                    arrayOf(1.0f, 1.0f, 1.0f),
                    arrayOf(0.0f, 0.0f, 0.0f),
                    arrayOf(0.0f, 0.0f, 0.0f),
                    arrayOf(0.0f, 0.0f, 0.0f),
                    0,
                    0
                )
                .spawnPacket(),
            sender
        )
        return true
      }
      else -> {
        sender.sendMessage("§cUsage: /textdisplay <create|delete|list|modify>")
        return true
      }
    }
  }
  // override onTabComplete(sender: CommandSender, command: Command, alias: String, args:
  // Array<String>): List<String> {
  // 		return listOf("create", "delete", "list", "modify")
  // }
}
