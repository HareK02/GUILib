package net.hareworks.guilib

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

data class Block(val length: Int, val lines: Int)

data class Vec2(val x: Int, val y: Int) {
  operator fun plus(other: Vec2): Vec2 {
    return Vec2(x + other.x, y + other.y)
  }
  operator fun minus(other: Vec2): Vec2 {
    return Vec2(x - other.x, y - other.y)
  }
}

class Builder(
    pos: Vec2 = Vec2(0, 0),
) {
  val start_position = Vec2(pos.x, -pos.y - 2)
  var current_position = Vec2(0, 0)
  var line_height = 0
  var component_builder =
      Component.text()
          .font(Key.key("minecraft:default"))
          .append(Component.text(String(Character.toChars(0xD0000 + (start_position.x * 3)))))

  fun append(element: CUIComponent): Builder {
    element.appendFor(this)
    return this
  }

  fun build(): TextComponent {
    return append(NewLine()).component_builder.build()
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

open class CUIComponentElement(text: String = "") : CUIComponent {
  override var width: Int = text.length
  override var height: Int = 1
  override var position: CUIComponent.Position = CUIComponent.Position.LEFT

  val text = text
  val length = text.length
  var color: TextColor? = NamedTextColor.WHITE

  val component: TextComponent
    get() = Component.text(text).color(color)

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
