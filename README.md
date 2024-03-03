# GUILib

PaperMC Plugin that gives you the way to create GUIs with ease.

This library is still under development. It is not recommended to use it in production.

## Usage

```kotlin
import net.hareworks.guilib.CUI

var bool1 = true
var number1 = 0
var number2 = 0

val gui = CUI(Vec2(-80, -30)) {
  register(
    Element("Test GUI").apply {
      width = 20
      position = CUIComponent.Position.CENTER
    },
    Element("v1.0").apply {
      width = 0
      position = CUIComponent.Position.RIGHT
    },
    NewLine(),
    Element("-".repeat(20)),
    NewLine(),
  )
  register(
    *arrayOf(
      Interactable("Toggle", ToggleButton(Ref({ bool1 }, { bool1 = it }))),
      Interactable("Number", Number(Ref({ number1 }, { number1 = it }), 0, 10)),
      Interactable("Slider", Slider(Ref({ number2 }, { number2 = it }), 0, 10)),
    ).also { it.forEach { it.width = 20 } }
  )
}
gui.open(player)
```
