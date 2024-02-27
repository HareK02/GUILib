package net.hareworks.guilib

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component as AdventureComponent
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

interface Element {
  var text: String

  var length: Int

  enum class Position {
    LEFT,
    CENTER,
    RIGHT
  }
  var position: Position
  var bold: Boolean?
  var italic: Boolean?
  var underlined: Boolean?
  var strikethrough: Boolean?
  var obfuscated: Boolean?
  var color: TextColor?

  fun component(): TextComponent
}

open class Component(text: String = "") : Element {
  override var text: String = text
    set(value) {
      field = value
      length = value.length
    }
  override var length: Int = text.length

  override fun component(): TextComponent {
    return AdventureComponent.text(text).decoration()
  }

  override var position: Element.Position = Element.Position.LEFT
  override var bold: Boolean? = false
  override var italic: Boolean? = false
  override var underlined: Boolean? = false
  override var strikethrough: Boolean? = false
  override var obfuscated: Boolean? = false
  override var color: TextColor? = NamedTextColor.WHITE
  open fun TextComponent.decoration(): TextComponent {
    if (bold != null) this.decoration(TextDecoration.BOLD, bold!!)
    if (italic != null) this.decoration(TextDecoration.ITALIC, italic!!)
    if (underlined != null) this.decoration(TextDecoration.UNDERLINED, underlined!!)
    if (strikethrough != null) this.decoration(TextDecoration.STRIKETHROUGH, strikethrough!!)
    if (obfuscated != null) this.decoration(TextDecoration.OBFUSCATED, obfuscated!!)
    if (color != null) this.color(color)
    return this
  }

  open fun onClick() {}
  open fun onUp() {}
  open fun onDown() {}
  open fun onHover() {}
  open fun onUnhover() {}
}

abstract class ActionComponent(text: String = "") : Component(text) {
  var isHovered: Boolean = false

  override var underlined: Boolean? = if (isHovered) true else false

  override fun component(): TextComponent {
    return AdventureComponent.text(text).decoration()
  }

  override open fun onHover() {
    isHovered = true
  }
  override open fun onUnhover() {
    isHovered = false
  }
  override open fun onClick() {}
}

open class Button(
    val variable: Ref<Boolean>,
) : ActionComponent("") {
  override var length: Int = 3
  override fun component(): TextComponent {
    return AdventureComponent.text("[" + (if (variable.value) "X" else " ") + "]").decoration()
  }

  override fun onClick() {
    variable.value = !variable.value
  }
}

class RadioButton(
    variable: Ref<Boolean>,
    val group: Ref<MutableList<RadioButton>>,
) : Button(variable) {
  init {
    group.value.add(this)
  }
  override fun onClick() {
    for (button in group.value) {
      button.variable.value = false
    }
    variable.value = true
  }
}

class NumberField(
    val variable: Ref<Int>,
) : ActionComponent("") {
  var range: IntRange = 0..99
    set(value) {
      field = value
      if (variable.value !in value) variable.value = value.first
      length = value.last.toString().length + 4
    }
  init {
    if (variable.value !in range) variable.value = range.first
  }
  override var length: Int = range.last.toString().length + 4
  override fun component(): TextComponent {
    return AdventureComponent.text(
            "< " + variable.value.toString().padStart(range.last.toString().length, '0') + " >"
        )
        .decoration()
  }

  override fun onClick() {
    variable.value = (variable.value + 1) % 100
  }
  override fun onUp() {
    variable.value = (variable.value + 1) % 100
  }
  override fun onDown() {
    variable.value = (variable.value - 1) % 100
    if (variable.value < 0) variable.value = 99
  }
}

open class NewLine : Element {
  override var text: String = ""
  override var length: Int = 0
  override var position: Element.Position = Element.Position.LEFT
  override var bold: Boolean? = false
  override var italic: Boolean? = false
  override var underlined: Boolean? = false
  override var strikethrough: Boolean? = false
  override var obfuscated: Boolean? = false
  override var color: TextColor? = NamedTextColor.WHITE

  override fun component(): TextComponent {
    return AdventureComponent.text("")
  }
}

class CUIBuilder(
    val pos: Pair<Int, Int> = Pair(0, 0),
) {
  val components: MutableList<Element> = mutableListOf()
  var start_pos = Pair(pos.first, -pos.second - 2)
  var current_position: Pair<Int, Int> = Pair(0, 0)
  var action_component_builder =
      AdventureComponent.text()
          .font(Key.key("minecraft:default"))
          .append(AdventureComponent.text(space(start_pos.first)))

  fun reset() {
    start_pos = Pair(pos.first, -pos.second - 2)
    current_position = Pair(0, 0)
    action_component_builder =
        AdventureComponent.text()
            .font(Key.key("minecraft:default"))
            .append(AdventureComponent.text(space(start_pos.first)))
  }

  fun append(element: Element) {
    if (element is NewLine) {
      action_component_builder.append(AdventureComponent.text(space(-current_position.first)))
      current_position = Pair(0, current_position.second + 1)
      return
    }

    val disp = (element.length - element.component().content().length)
    val odd = (disp % 2 == 1)
    val overflown = disp < 0
    if (element.position == Element.Position.CENTER) {
      action_component_builder
          .append(
              AdventureComponent.text(space(disp / 2) + (if (odd) halfSpace(!overflown) else ""))
          )
          .append(element.component().line(current_position.second))
          .append(
              AdventureComponent.text(space(disp / 2) + (if (odd) halfSpace(!overflown) else ""))
          )
    } else if (element.position == Element.Position.RIGHT) {
      action_component_builder
          .append(AdventureComponent.text(space(disp)))
          .append(element.component().line(current_position.second))
    } else {
      action_component_builder
          .append(element.component().line(current_position.second))
          .append(AdventureComponent.text(space(disp)))
    }
    current_position = Pair(current_position.first + element.length, current_position.second)
  }
  private fun AdventureComponent.line(line: Int): AdventureComponent {
    return this.font(Key.key("guitoolkit:text/${line*3 - (start_pos.second + 3)}"))
  }
  private fun space(width: Int): String {
    return String(Character.toChars(0xD0000 + (width * 3)))
  }
  private fun halfSpace(positive: Boolean): String {
    return (if (positive) "\uDB00\uDC01" else "\uDAFF\uDFFF") +
        String(Character.toChars(0x50000 + if (positive) 2401 else -2401))
  }

  fun render(): AdventureComponent {
    reset()
    for (component in components) {
      append(component)
    }
    append(NewLine())
    return action_component_builder.build()
  }
}
