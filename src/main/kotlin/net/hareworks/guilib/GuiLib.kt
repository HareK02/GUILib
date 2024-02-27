package net.hareworks.guilib

import com.destroystokyo.paper.event.player.PlayerJumpEvent
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
    val cooldown = HashMap<Player, Int>()
  }

  val ticker =
      object : BukkitRunnable() {
        override fun run() {
          instances.forEach { (player, gui) ->
            val from = locationcash[player]
            if (from != null && (cooldown[player] == 0 || cooldown[player] == null)) {
              val to = Location(player.location)
              var a = to - from
              val amount = Math.sqrt(a.x * a.x + a.z * a.z)
              locationcash[player] = to
              if (amount > 0.05) {
                var degree = Math.toDegrees(Math.atan2(-a.x, a.z)) - player.location.yaw
                if (degree > 180) degree -= 360
                if (degree < -180) degree += 360
                when (degree) {
                  in -45.0..45.0 -> gui.moveUp()
                  in 45.0..135.0 -> gui.moveRight()
                  in -135.0..-45.0 -> gui.moveLeft()
                  in 135.0..180.0, in -180.0..-135.0 -> gui.moveDown()
                }
                cooldown[player] = 3
              }

              if (!gui.capture) {
                if (a.yaw > 180) a.yaw -= 360
                if (a.yaw < -180) a.yaw += 360
                if (a.pitch > 180) a.pitch -= 360
                if (a.pitch < -180) a.pitch += 360
                if (!(a.yaw in -1.0..1.0 && a.pitch in -1.0..1.0))
                    gui.moveCursor(a.yaw.toInt(), a.pitch.toInt())
              }
            }
            if (cooldown[player] != 0) cooldown[player] = cooldown[player]!! - 1
            gui.update()
          }
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
    cooldown.remove(e.player)
  }

  @EventHandler
  public fun onPlayerInteract(e: PlayerInteractEvent) {
    val a = e.getAction()
    when (a) {
      Action.LEFT_CLICK_AIR, Action.LEFT_CLICK_BLOCK -> this.onLeftClick(e.player)
      Action.RIGHT_CLICK_AIR, Action.RIGHT_CLICK_BLOCK -> this.onRightClick(e.player)
      else -> {}
    }
  }
  @EventHandler
  public fun onPlayerInteractEntity(e: PlayerInteractEntityEvent) {
    this.onRightClick(e.player)
  }
  @EventHandler
  public fun onPrePlayerAttackEntity(e: PrePlayerAttackEntityEvent) {
    this.onLeftClick(e.player)
  }

  private val lmap = HashMap<Player, BukkitRunnable>()
  private fun onLeftClick(player: Player) {
    if (lmap.containsKey(player)) return
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
    }
  }

  private val rmap = HashMap<Player, BukkitRunnable>()
  private fun onRightClick(player: Player) {
    if (rmap.containsKey(player)) return
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
    }
  }

  @EventHandler
  public fun onPlayerJump(e: PlayerJumpEvent) {
    if (instances.containsKey(e.player)) {
      e.isCancelled = true
      instances[e.player]!!.enter()
    }
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
  var cursor = Pair(0, 0)

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

  public fun leftClick() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun rightClick() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun moveLeft() {
    player.sendActionBar(Component.text("moveLeft"))
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun moveRight() {
    player.sendActionBar(Component.text("moveRight"))
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun moveUp() {
    player.sendActionBar(Component.text("moveUp"))
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun moveDown() {
    player.sendActionBar(Component.text("moveDown"))
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun enter() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
  }
  public fun quit() {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    GuiLib.instances.remove(player)
  }
}

class TestGUI(player: Player) : GUI(player) {
  var bool1 = true
  var number1 = 0
  val window =
      CUIBuilder(pos = Pair(-80, -40)).apply {
        components.apply {
          add(Component("TestGUI ").apply { bold = true })
          add(Component("v1.0.0").apply { color = NamedTextColor.GRAY })
          add(NewLine())
          add(Component(" Button1 "))
          add(
              Button(Ref({ bool1 }, { bool1 = it })).apply {
                bold = true
                length = 6
                position = Element.Position.CENTER
              }
          )
          add(NewLine())
          add(Component(" Number1 "))
          add(
              NumberField(Ref({ number1 }, { number1 = it })).apply {
                bold = true
                length = 6
                position = Element.Position.CENTER
              }
          )
        }
      }

  init {
    GuiLib.instances[player] = this
    GuiLib.locationcash[player] = Location(player.location)
  }

  override fun render(): Component {
    return window.render()
  }
}

class Ref<T>(private val get: () -> T, private val set: (value: T) -> Unit) {
  var value: T
    get() = get()
    set(value) = set(value)
}
