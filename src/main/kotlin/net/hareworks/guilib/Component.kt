package net.hareworks.guilib

import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.TextComponent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor

data class Block(val length: Int, val lines: Int)

data class Vec2(val x: Int, val y: Int)

data class Content(val text: String, val length: Int, val lines: Int)

class Builder(
    pos: Vec2 = Vec2(0, 0),
) {
  val start_position = Vec2(pos.x, -pos.y - 2)
  var current_position = Vec2(0, 0)
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
      current_position = Vec2(0, builder.current_position.y + 1)
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
  }
}

// abstract class ActionComponent(text: String = "") : Component(text) {
//   var isHovered: Boolean = false

//   override fun component(): TextComponent {
//     return Component.text(text).decoration()
//   }

//   override open public fun onHover() {
//     isHovered = true
//   }
//   override open public fun onUnhover() {
//     isHovered = false
//   }
//   override open public fun onClickLeft() {}
//   override open public fun onClickRight() {}
// }

// open class Button(
//     val variable: Ref<Boolean>,
// ) : ActionComponent("") {
//   override var length: Int = 3
//   override fun component(): TextComponent {
//     return Component.text("[" + (if (variable.value) "X" else " ") + "]").decoration()
//   }

//   override fun onClickLeft() {
//     variable.value = !variable.value
//   }
//   override fun onClickRight() {
//     onClickLeft()
//   }
// }

// class RadioButton(
//     variable: Ref<Boolean>,
//     val group: Ref<MutableList<RadioButton>>,
// ) : Button(variable) {
//   init {
//     group.value.add(this)
//   }
//   override fun onClickLeft() {
//     for (button in group.value) {
//       button.variable.value = false
//     }
//     variable.value = true
//   }
// }

// class NumberField(
//     val variable: Ref<Int>,
// ) : ActionComponent("") {
//   var range: IntRange = 0..99
//     set(value) {
//       field = value
//       if (variable.value !in value) variable.value = value.first
//       length = value.last.toString().length + 4
//     }
//   init {
//     if (variable.value !in range) variable.value = range.first
//   }
//   override var length: Int = range.last.toString().length + 4
//   override fun component(): TextComponent {
//     return Component.text(
//             "< " + variable.value.toString().padStart(range.last.toString().length, '0') + " >"
//         )
//         .decoration()
//   }

//   override fun onClickLeft() {
//     variable.value = (variable.value + 1) % 100
//   }
//   override fun onClickRight() {
//     variable.value = (variable.value - 1) % 100
//     if (variable.value < 0) variable.value = 99
//   }
// }

// class GridBox(
//     val components: List<List<CUIComponent>>,
// ) : CUIComponent {
//   override var width: Int = components.map { it.map { it.width }.sum() }.maxOrNull() ?: 0
//     get() = components.map { it.map { it.width }.sum() }.maxOrNull() ?: 0
//   override var height: Int = components.map { it.map { it.height }.sum() }.maxOrNull() ?: 0
//     get() = components.map { it.map { it.height }.sum() }.maxOrNull() ?: 0
//   override var position: CUIComponent.Position = CUIComponent.Position.LEFT

//   override fun appendFor(builder: Builder) {
//     for (line in components) {
//       for (component in line) {
//         component.appendFor(builder)
//       }
//       builder.component_builder.append(Component.text(space(-builder.current_position.first)))
//       builder.current_position = Pair(0, builder.current_position.second + 1)
//     }
//   }
// }

  // override fun leftClick() {
  //   val targets = components.filter { it is ActionComponent }.map { it as ActionComponent }
  //   if (targets.isEmpty()) return
  //   targets[selected].onClickLeft()
  // }
  // override fun rightClick() {
  //   val targets = components.filter { it is ActionComponent }.map { it as ActionComponent }
  //   if (targets.isEmpty()) return
  //   targets[selected].onClickRight()
  // }
  // override fun scroll(up: Boolean) {
  //   val targets = components.filter { it is ActionComponent }.map { it as ActionComponent }
  //   if (targets.isEmpty()) return
  //   targets[selected].onUnhover()
  //   selected = Math.max(0, Math.min(if (up) selected - 1 else selected + 1, targets.size - 1))
  //   targets[selected].onHover()
  // }
