package net.hareworks.guilib

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import org.bukkit.entity.Player
import org.checkerframework.checker.units.qual.min

internal data class Block(val length: Int, val lines: Int)

public data class Vec2(val x: Int, val y: Int) {
  operator fun plus(other: Vec2): Vec2 {
    return Vec2(x + other.x, y + other.y)
  }
  operator fun minus(other: Vec2): Vec2 {
    return Vec2(x - other.x, y - other.y)
  }
}

public class Builder(
    pos: Vec2 = Vec2(0, 0),
) {
  val start_position = Vec2(pos.x, -pos.y - 2)
  var current_position = Vec2(0, 0)
  var line_height = 0
  var component_builder =
      Component.text()
          .font(Key.key("minecraft:default"))
          .append(Component.text(String(Character.toChars(0xD0000 + (start_position.x * 3)))))

  val components: MutableList<CUIComponent> = mutableListOf()
  fun register(element: CUIComponent): Builder {
    components.add(element)
    return this
  }
  fun register(vararg elements: CUIComponent): Builder {
    elements.forEach { components.add(it) }
    return this
  }

  fun append(element: CUIComponent): Builder {
    element.appendFor(this)
    return this
  } 
  fun append(vararg elements: CUIComponent): Builder {
    elements.forEach { it.appendFor(this) }
    return this
  }

  fun build(): TextComponent {
    current_position = Vec2(0, 0)
    line_height = 0
    component_builder =
        Component.text()
            .font(Key.key("minecraft:default"))
            .append(Component.text(String(Character.toChars(0xD0000 + (start_position.x * 3)))))
    components.forEach { it.appendFor(this) }
    NewLine().appendFor(this)
    return component_builder.build()
  }
}

interface CUIComponent {
  enum class Position {
    LEFT,
    CENTER,
    RIGHT
  }

  var width: Int
  var height: Int
  var position: Position

  fun appendFor(builder: Builder)

  fun Component.line(line: Int, offset: Int): Component {
    return this.font(Key.key("guitoolkit:text/${line*3 - (offset + 3)}"))
  }
  fun space(width: Int): String {
    return String(Character.toChars(0xD0000 + (width * 3)))
  }
  fun halfSpace(positive: Boolean): String {
    return (if (positive) "\uDB00\uDC01" else "\uDAFF\uDFFF") +
        String(Character.toChars(0x50000 + if (positive) 2401 else -2401))
  }
}

class NewLine : CUIComponent {
  override var width: Int = 0
  override var height: Int = 1
  override var position: CUIComponent.Position = CUIComponent.Position.LEFT
  override fun appendFor(builder: Builder) {
    builder.apply {
      component_builder.append(Component.text(space(-builder.current_position.x)))
      current_position = Vec2(0, builder.current_position.y + builder.line_height)
      line_height = 0
    }
  }
}

open class Element : CUIComponent {
  constructor(text: String = "") {
    this.component = Component.text(text)
    length = text.length
  }
  constructor(component: TextComponent) {
    this.component = component
    length = component.content().length
  }
  var length: Int = 0
  var color: TextColor? = NamedTextColor.WHITE
  open var component: TextComponent = Component.text("").color(color)

  override var width: Int = length
  override var height: Int = 1
  override var position: CUIComponent.Position = CUIComponent.Position.LEFT

  override fun appendFor(builder: Builder) {
    val disp = (width - component.content().length)
    val odd = (disp % 2 == 1)
    val overflown = disp < 0
    if (position == CUIComponent.Position.CENTER)
        builder
            .component_builder
            .append(Component.text(space(disp / 2) + (if (odd) halfSpace(!overflown) else "")))
            .append(
                component.line(builder.current_position.y, builder.start_position.y) as
                    TextComponent
            )
            .append(Component.text(space(disp / 2) + (if (odd) halfSpace(!overflown) else "")))
    else if (position == CUIComponent.Position.RIGHT)
        builder
            .component_builder
            .append(Component.text(space(disp)))
            .append(
                component.line(builder.current_position.y, builder.start_position.y) as
                    TextComponent
            )
    else
        builder
            .component_builder
            .append(
                component.line(builder.current_position.y, builder.start_position.y) as
                    TextComponent
            )
            .append(Component.text(space(disp)))
    builder.current_position = Vec2(builder.current_position.x + width, builder.current_position.y)
    builder.line_height = Math.max(builder.line_height, height)
  }
}

abstract class ActionElement : Element() {
  abstract fun onClickLeft(player: Player)
  abstract fun onClickRight(player: Player)
}

open class ToggleButton(
    value: Ref<Boolean>,
) : ActionElement() {
  val ref: Ref<Boolean> = value
  override fun onClickLeft(player: Player) {
    ref.value = !ref.value
  }
  override fun onClickRight(player: Player) {
    ref.value = !ref.value
  }

  override var component: TextComponent
    get() = Component.text("[" + (if (ref.value) "X" else " ") + "]").color(color)
    set(_) {}
}

open class Number(
    val value: Ref<Int>,
    var min: Int,
    var max: Int,
) : ActionElement() {
  fun increment() {
    value.value = (value.value + 1) % (max + 1)
  }
  fun decrement() {
    value.value = (value.value - 1 + (max + 1)) % (max + 1)
  }
  override fun onClickLeft(player: Player) {
    decrement()
  }
  override fun onClickRight(player: Player) {
    increment()
  }
  override var component: TextComponent
    get() = Component.text(value.value.toString()).color(color)
    set(_) {}
}

open class Slider(
    value: Ref<Int>,
    min: Int,
    max: Int,
) : Number(value, min, max) {
  private fun meter(width: Int, value: Int, min: Int, max: Int): String {
    val meter =
        ((value - min).toFloat() / (max - min).toFloat() * (width - 4 - max.toString().length))
            .toInt()
    return " [${"=".repeat(meter).padEnd(width - 4 - max.toString().length, ' ')}] ${value.toString().padStart(max.toString().length, ' ')}"
  }

  override var component: TextComponent
    get() = Component.text(meter(width, value.value, min, max)).color(color)
    set(_) {}
}

open class Anchor(
    val text: String,
    val page: GUI,
) : ActionElement() {
  override var component: TextComponent
    get() = Component.text("<$text>").color(color)
    set(_) {}

  override fun onClickLeft(player: Player) {
    page.open(player)
  }
  override fun onClickRight(player: Player) {
    page.open(player)
  }
}

class Interactable(
    label: String,
    component: ActionElement,
) : CUIComponent {
  val label: String = label
  val element: ActionElement = component

  override var width: Int = 0
    set(value) {
      field = maxOf(label.length + 1, value)
      element.width = maxOf(0, value - label.length - 1)
    }
  override var height: Int = 1
  override var position: CUIComponent.Position = CUIComponent.Position.LEFT
  init {
    element.position = CUIComponent.Position.RIGHT
  }
  var hoverd = false

  override fun appendFor(builder: Builder) {
    builder.apply {
      append(
          Element((if (hoverd) " " else "") + label).apply {
            this.width = label.length + 1
            position = CUIComponent.Position.LEFT
          },
          element,
          NewLine()
      )
    }
  }
}
