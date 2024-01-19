package net.hareworks.simplytextdisplay

import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.PacketContainer
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import java.lang.reflect.InvocationTargetException

public class SimplyTextDisplay : JavaPlugin() {
  companion object {
    lateinit var plugin: SimplyTextDisplay
      private set
    var hasPlaceholderAPI = false
      private set
    var protocolManager: ProtocolManager? = null
      private set
  }
	var textDisplays = mutableListOf<TextDisplay>()

  override fun onEnable() {
    plugin = this
    getCommand("textdisplay")?.setExecutor(Command())
    protocolManager = ProtocolLibrary.getProtocolManager()
    hasPlaceholderAPI = server.pluginManager.getPlugin("PlaceholderAPI") != null

		server.scheduler.scheduleSyncRepeatingTask(this, {
			update()
		}, 0, 1)
  }

	fun update() {
		for (textDisplay in textDisplays) {
			textDisplay.update()
		}
	}

	public fun send(packet: PacketContainer, player: Player) {
		try {
			protocolManager?.sendServerPacket(player, packet)
		} catch (e: InvocationTargetException) {
			throw RuntimeException("Cannot send packet.", e)
		}
	}

	override fun onDisable() {
		// Plugin shutdown logic
	}
}
