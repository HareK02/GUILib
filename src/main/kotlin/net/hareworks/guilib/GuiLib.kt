package net.hareworks.guilib

import io.papermc.paper.event.player.PrePlayerAttackEntityEvent
import java.time.Duration
import net.kyori.adventure.key.Key
import net.kyori.adventure.sound.Sound
import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.Action
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable

class GuiLib : JavaPlugin(), Listener {
  companion object {
    internal lateinit var instance: GuiLib
  }

  private val ticker =
      object : BukkitRunnable() {
        override fun run() {
          GUI.instances.forEach { (_, gui) -> gui.update() }
        }
      }
  override fun onEnable() {
    instance = this
    server.pluginManager.registerEvents(this, this)
    ticker.runTaskTimer(this, 1L, 1L)
  }

  override fun onDisable() {
    ticker.cancel()
  }

  @EventHandler
  private fun onPlayerLeave(e: org.bukkit.event.player.PlayerQuitEvent) {
    GUI.instances.remove(e.player)
  }

  @EventHandler
  private fun onPlayerInteract(e: PlayerInteractEvent) {
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
  private fun onPlayerInteractEntity(e: PlayerInteractEntityEvent) {
    if (this.onRightClick(e.player)) e.isCancelled = true
  }
  @EventHandler
  private fun onPrePlayerAttackEntity(e: PrePlayerAttackEntityEvent) {
    if (this.onLeftClick(e.player)) e.isCancelled = true
  }
  @EventHandler
  private fun onPlayerThrowItem(e: org.bukkit.event.player.PlayerDropItemEvent) {
    if (GUI.instances.containsKey(e.player)) e.isCancelled = true
  }
  @EventHandler
  private fun onInventoryClick(e: org.bukkit.event.inventory.InventoryClickEvent) {
    if (GUI.instances.containsKey(e.whoClicked)) {
      if (e.slot == 4) {
        e.isCancelled = true
      }
    }
  }
  @EventHandler
  private fun onPlayerItemHeld(e: org.bukkit.event.player.PlayerItemHeldEvent) {
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
    if (GUI.instances.containsKey(player)) {
      GUI.instances[player]!!.leftClick(player)
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
    if (GUI.instances.containsKey(player)) {
      GUI.instances[player]!!.rightClick(player)
      return true
    }
    return false
  }
  private fun onScroll(player: Player, up: Int): Boolean {
    if (GUI.instances.containsKey(player)) {
      if (up > 0) {
        GUI.instances[player]!!.scrollUp(player)
      } else if (up < 0) {
        GUI.instances[player]!!.scrollDown(player)
      }
      return true
    }
    return false
  }

  @EventHandler
  private fun onPlayerToggleSneak(e: PlayerToggleSneakEvent) {
    if (GUI.instances.containsKey(e.player) && e.isSneaking) {
      GUI.instances[e.player]!!.quit()
    }
  }
}

abstract class GUI() {
  companion object {
    data class Hotbar(val items: Array<ItemStack?>, val heldSlot: Int) {
      constructor(
          player: Player
      ) : this(Array(9) { player.inventory.getItem(it)?.clone() }, player.inventory.heldItemSlot)
      fun apply(player: Player) {
        items.forEachIndexed { i, item -> player.inventory.setItem(i, item) }
        player.inventory.heldItemSlot = heldSlot
      }
    }
    internal val instances: HashMap<Player, GUI> = HashMap()
    internal val hotbars: HashMap<Player, Hotbar> = HashMap()
  }

  protected var previous: GUI? = null
  protected var player: Player? = null
  public fun open(player: Player) {
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    this.player = player
    if (GUI.instances.containsKey(player) && GUI.instances[player]?.previous != this) {
      previous = GUI.instances[player]
      previous?.player = null
      GuiLib.instance.logger.info("$previous ${previous?.previous} ${previous?.previous?.previous}")
    } else {
      GUI.hotbars[player] = Hotbar(player)
      player.inventory.heldItemSlot = 4
      (0..8).forEach {
        player.inventory.setItem(
            it,
            org.bukkit.inventory.ItemStack(org.bukkit.Material.STICK).apply {
              itemMeta =
                  itemMeta.apply {
                    displayName(Component.text(" "))
                    setCustomModelData(7201)
                  }
            }
        )
      }
    }
    GUI.instances[player] = this
  }
  public fun quit() {
    player?.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    if (previous != null) {
      previous?.open(player!!)
      previous = null
      player = null
    } else {
      GUI.hotbars[player]?.apply(player!!)
      GUI.instances.remove(player)
    }
  }
  internal fun update() {
    if (player == null) return
    val title =
        Title.title(
            Component.text(""),
            render(),
            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(200), Duration.ofMillis(100))
        )
    player?.showTitle(title)
  }
  abstract protected fun render(): Component

  abstract internal fun leftClick(player: Player)
  abstract internal fun rightClick(player: Player)
  abstract internal fun scrollUp(player: Player)
  abstract internal fun scrollDown(player: Player)
}

public class CUI(position: Vec2, block: Builder.() -> Builder) : GUI() {
  var builder: Builder = Builder(position).block()
  var interactables = builder.components.filterIsInstance<Interactable>()
  var selected: Int = 0
    set(value) {
      field = value
      interactables.forEach { it.hoverd = false }
      interactables[value].hoverd = true
    }
  init {
    selected = 0
  }
  override fun render(): Component {
    return builder.build()
  }
  override fun leftClick(player: Player) {
    interactables[selected].onClickLeft(player)
  }
  override fun rightClick(player: Player) {
    interactables[selected].onClickRight(player)
  }
  override fun scrollUp(player: Player) {
    scroll(true)
  }
  override fun scrollDown(player: Player) {
    scroll(false)
  }
  private fun scroll(up: Boolean) {
    val range = interactables.size
    if (!up && selected < range - 1) {
      player?.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 0.8f))
      selected++
    } else if (up && selected > 0) {
      player?.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 0.8f))
      selected--
    } else {
      player?.playSound(
          Sound.sound(Key.key("block.note_block.bass"), Sound.Source.MASTER, 0.6f, 0.8f)
      )
      return
    }
  }
}

public class Ref<T>(private val get: () -> T, private val set: (value: T) -> Unit) {
  var value: T
    get() = get()
    set(value) = set(value)
}
