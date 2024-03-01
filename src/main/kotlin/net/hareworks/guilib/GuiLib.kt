package net.hareworks.guilib

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import java.time.Duration
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

data class Location(var x: Double, var y: Double, var z: Double, var yaw: Float, var pitch: Float) {
  operator fun minus(other: Location): Location {
    return Location(x - other.x, y - other.y, z - other.z, yaw - other.yaw, pitch - other.pitch)
  }
  operator fun plus(other: Location): Location {
    return Location(x + other.x, y + other.y, z + other.z, yaw + other.yaw, pitch + other.pitch)
  }

  constructor(
      location: org.bukkit.Location
  ) : this(location.x, location.y, location.z, location.yaw, location.pitch)
}

class GuiLib : JavaPlugin(), Listener {
  companion object {
    lateinit var instance: GuiLib
    val instances: HashMap<Player, GUI> = HashMap()
    val locationcash: HashMap<Player, Location> = HashMap()
  }

  val ticker =
      object : BukkitRunnable() {
        override fun run() {
          instances.forEach { (_, gui) -> gui.update() }
        }
      }
  override fun onEnable() {
    instance = this
    server.pluginManager.registerEvents(this, this)
    ticker.runTaskTimer(this, 1L, 1L)
    getCommand("guilib")?.setExecutor { sender, command, label, args ->
      if (sender is Player) {
        TestGUI(sender)
      }
      true
    }
  }
  override fun onDisable() {
    ticker.cancel()
  }

  @EventHandler
  public fun onPlayerLeave(e: org.bukkit.event.player.PlayerQuitEvent) {
    instances.remove(e.player)
    locationcash.remove(e.player)
  }

  @EventHandler
  public fun onPlayerInteract(e: PlayerInteractEvent) {
    val a = e.getAction()
    when (a) {
      Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK ->
          if (this.onLeftClick(e.player)) e.isCancelled = true
      Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK ->
          if (this.onRightClick(e.player)) e.isCancelled = true
      else -> {}
    }
  }
  @EventHandler
  public fun onPlayerInteractEntity(e: PlayerInteractEntityEvent) {
    if (this.onRightClick(e.player)) e.isCancelled = true
  }
  @EventHandler
  public fun onPrePlayerAttackEntity(e: PrePlayerAttackEntityEvent) {
    if (this.onLeftClick(e.player)) e.isCancelled = true
  }
  @EventHandler
  public fun onPlayerThrowItem(e: org.bukkit.event.player.PlayerDropItemEvent) {
    if (instances.containsKey(e.player)) e.isCancelled = true
  }
  @EventHandler
  public fun onInventoryClick(e: org.bukkit.event.inventory.InventoryClickEvent) {
    if (instances.containsKey(e.whoClicked)) {
      if (e.slot == 4) {
        e.isCancelled = true
      }
    }
  }
  @EventHandler
  public fun onPlayerItemHeld(e: org.bukkit.event.player.PlayerItemHeldEvent) {
    if (this.onScroll(e.player, e.newSlot - e.previousSlot)) e.isCancelled = true
  }

  private val lmap = HashMap<Player, BukkitRunnable>()
  private fun onLeftClick(player: Player): Boolean {
    if (lmap.containsKey(player)) return false
    lmap.put(
        player,
        object : BukkitRunnable() {
              override fun run() {
                lmap.remove(player)
              }
            }
            .also { it.runTaskLater(this, 1L) }
    )
    if (instances.containsKey(player)) {
      instances[player]!!.leftClick()
      return true
    }
    return false
  }
  private val rmap = HashMap<Player, BukkitRunnable>()
  private fun onRightClick(player: Player): Boolean {
    if (rmap.containsKey(player)) return false
    rmap.put(
        player,
        object : BukkitRunnable() {
              override fun run() {
                rmap.remove(player)
              }
            }
            .also { it.runTaskLater(this, 1L) }
    )
    if (instances.containsKey(player)) {
      instances[player]!!.rightClick()
      return true
    }
    return false
  }

  private fun onScroll(player: Player, up: Int): Boolean {
    if (instances.containsKey(player)) {
      instances[player]!!.scroll(up > 0)
      return true
    }
    return false
  }

  @EventHandler
  public fun onPlayerToggleSneak(e: PlayerToggleSneakEvent) {
    if (instances.containsKey(e.player)) {
      e.isCancelled = true
      instances[e.player]!!.quit()
    }
  }
}

abstract class GUI(val player: Player) {
  var capture = false
  var cursor = Pair(0, 0) // x, y
  val heldItem = player.inventory.getItem(4)
  val heldSlot = player.inventory.heldItemSlot
  init {
    player.inventory.heldItemSlot = 4
    player.inventory.setItemInMainHand(
        org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK).apply {
          itemMeta =
              itemMeta.apply {
                displayName(Component.text(" "))
                setCustomModelData(7201)
              }
        }
    )
  }

  private fun sendActionBar(text: Component) {
    player.sendActionBar(text)
  }
  public fun update() {
    val title =
        Title.title(
            Component.text(""),
            render(),
            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(200), Duration.ofMillis(100))
        )

    player.showTitle(title)
  }
  abstract fun render(): Component

  public fun capture() {
    capture = true
  }
  public fun release() {
    capture = false
  }
  public fun moveCursor(x: Int, y: Int) {
    cursor = Pair(cursor.first + x, cursor.second + y)
  }

  abstract fun leftClick()
  abstract fun rightClick()
  abstract fun scroll(up: Boolean)

  public fun enter() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun quit() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    player.inventory.setItemInMainHand(heldItem)
    player.inventory.heldItemSlot = heldSlot
    GuiLib.instances.remove(player)
  }
}

class TestGUI(player: Player) : GUI(player) {
  var bool1 = true
  var number1 = 0
  var builder: Builder =
      Builder(Vec2(0, 0)).apply {
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("GUILib!").apply {
              width = 16
              position = CUIComponent.Position.LEFT
            }
        )
        append(CUIComponentElement("|"))
        append(NewLine())
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("GUILib!").apply {
              width = 16
              position = CUIComponent.Position.CENTER
            }
        )
        append(CUIComponentElement("|"))
        append(NewLine())
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("GUILib!").apply {
              width = 16
              position = CUIComponent.Position.RIGHT
            }
        )
        append(CUIComponentElement("|"))
        append(NewLine())
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("Smart & Powerful Library!!").apply {
              width = 16
              color = NamedTextColor.GREEN
              position = CUIComponent.Position.LEFT
            }
        )
        append(CUIComponentElement("|"))
        append(NewLine())
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("Smart & Powerful Library!!").apply {
              width = 16
              color = NamedTextColor.GREEN
              position = CUIComponent.Position.CENTER
            }
        )
        append(CUIComponentElement("|"))
        append(NewLine())
        append(CUIComponentElement("|"))
        append(
            CUIComponentElement("Smart & Powerful Library!!").apply {
              width = 16
              color = NamedTextColor.GREEN
              position = CUIComponent.Position.RIGHT
            }
        )
        append(CUIComponentElement("|"))
      }

  init {
    GuiLib.instances[player] = this
  }

  override fun render(): Component {
    return builder.build()
  }

  override fun leftClick() {}

  override fun rightClick() {}

  override fun scroll(up: Boolean) {}
}

class Ref<T>(private val get: () -> T, private val set: (value: T) -> Unit) {
  var value: T
    get() = get()
    set(value) = set(value)
}
