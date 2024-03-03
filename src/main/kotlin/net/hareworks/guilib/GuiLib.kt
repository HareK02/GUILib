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
          GUI.instances.forEach { it.update() }
        }
      }
  override fun onEnable() {
    instance = this
    server.pluginManager.registerEvents(this, this)
    ticker.runTaskTimer(this, 1L, 1L)
  }

  override fun onDisable() {
    GUI.instances.forEach { it.forceQuit() }
    ticker.cancel()
  }

  @EventHandler
  private fun onPlayerLeave(e: org.bukkit.event.player.PlayerQuitEvent) {
    GUI.forceQuit(e.player)
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
    if (GUI.has(e.player)) e.isCancelled = true
  }
  @EventHandler
  private fun onInventoryClick(e: org.bukkit.event.inventory.InventoryClickEvent) {
    if (GUI.has(e.whoClicked as Player)) {
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
    val gui = GUI.get(player)
    if (gui == null) return false
    gui.leftClick(player)
    return true
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
    val gui = GUI.get(player)
    if (gui == null) return false
    gui.rightClick(player)
    return true
  }
  private fun onScroll(player: Player, up: Int): Boolean {
    val gui = GUI.get(player)
    if (gui != null) {
      if (up > 0) {
        gui.scrollUp(player)
      } else if (up < 0) {
        gui.scrollDown(player)
      }
      return true
    }
    return false
  }

  @EventHandler
  private fun onPlayerToggleSneak(e: PlayerToggleSneakEvent) {
    val gui = GUI.get(e.player)
    if (gui != null && e.isSneaking) {
      gui.quit()
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
    internal val instances: MutableList<GUI> = ArrayList()
    internal val hotbars: HashMap<Player, Hotbar> = HashMap()
    fun forceQuit(player: Player) {
      if (instances.firstNotNullOf { it.player } == player) {
        hotbars[player]?.apply(player)
        hotbars.remove(player)
        instances.filter { it.player == player }.forEach { instances.remove(it) }
      }
    }
    fun has(player: Player): Boolean {
      return instances.any { it.player == player }
    }
    fun get(player: Player): GUI? {
      return instances.firstOrNull { it.player == player }
    }
  }

  protected var player: Player? = null
  protected var enabled: Boolean = true

  protected var previous: GUI? = null

  fun open(player: Player) {
    this.enabled = true
    player.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    if (GUI.has(player)) {
      GuiLib.instance.logger.info("player has gui")
      val opened = GUI.get(player)!!
      if (opened.previous == this) {
        GuiLib.instance.logger.info("previous == this")
        opened.previous = null
      } else {
        GuiLib.instance.logger.info("store previous")
        this.previous = opened
      }
      opened.enabled = false
      GUI.instances.remove(opened)
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
    this.player = player
    GUI.instances.add(this)
  }
  fun quit() {
    if (player == null) return
    this.enabled = false
    player?.playSound(Sound.sound(Key.key("ui.button.click"), Sound.Source.MASTER, 0.6f, 1.2f))
    if (previous != null) {
      GuiLib.instance.logger.info("quit → previous open")
      previous?.open(player!!)
      previous = null
    } else {
      GUI.hotbars[player]?.apply(player!!)
      GUI.hotbars.remove(player)
      GUI.instances.remove(this)
    }
  }
  fun forceQuit() {
    if (player == null) return
    GUI.hotbars[player]?.apply(player!!)
    GUI.hotbars.remove(player)
    GUI.instances.remove(this)
  }
  internal fun update() {
    if (!enabled) return
    player?.showTitle(
        Title.title(
            Component.text(""),
            render(),
            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(200), Duration.ofMillis(100))
        )
    )
  }
  abstract protected fun render(): Component

  abstract internal fun leftClick(player: Player)
  abstract internal fun rightClick(player: Player)
  abstract internal fun scrollUp(player: Player)
  abstract internal fun scrollDown(player: Player)
}

class CUI(position: Vec2, block: Builder.() -> Builder) : GUI() {
  var builder: Builder = Builder(position).block()
  var interactables = builder.components.filterIsInstance<Interactable>()
  var selected: Int = 0
    set(value) {
      field = value
      if (interactables.isEmpty()) return
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
    scroll(true, player)
  }
  override fun scrollDown(player: Player) {
    scroll(false, player)
  }
  private fun scroll(up: Boolean, player: Player? = null) {
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

class Ref<T>(private val get: () -> T, private val set: (value: T) -> Unit) {
  var value: T
    get() = get()
    set(value) = set(value)
}
