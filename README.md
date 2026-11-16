# CS2 Knife Menu (client-side Fabric mod, 1.21.11)

Same knives, textures, animations and sounds as *CS2 Knives for LR Tactical*, but it does **not need TACZ,
LR Tactical or anything on the server**. It's a client-only visual mod: when you hold a sword (configurable),
the vanilla hand is replaced by your chosen CS2 knife with its draw / slash / inspect animations.

## Use
| | |
|---|---|
| **K** | open the menu (rebind inside the menu) |
| **Y** | inspect (rebind inside the menu) |
| Left click | slash 1 / 2 (alternating) |
| Right click | heavy slash |

Menu: icon grid (**Knives** / **Skins** tabs), click to equip. **Tune** tab = position, yaw/pitch, size, volume,
left-hand mirror, reset. Bottom bar: *Show* (Swords / Any item / Always), On/Off, Inspect.
Settings are saved in `config/cs2knifemenu.json`.

## Build (the code has NOT been compiled yet - read "Version notes")
Easiest, most reliable way:
1. Go to <https://fabricmc.net/develop>, pick **Minecraft 1.21.11**, **Mojang mappings**, mod id `cs2knifemenu`,
   download the template.
2. Copy this project's `src/` folder and `tools/` over the template's `src/` (merge, replace) and use the
   template's `build.gradle` / `gradle.properties` (they have the right Loom, Loader and Fabric API versions).
3. `./gradlew build` -> jar in `build/libs/`. Drop it in your `mods` folder (needs Fabric API).

The `build.gradle` / `gradle.properties` included here are a best guess; if you prefer them, check the version
numbers on the Fabric site. A GitHub Actions workflow is included if you'd rather build in the cloud.

## Version notes (where it might need a one-line fix)
Only two files touch Minecraft's rendering internals and a few names changed in 1.21.9-1.21.11:
* `mixin/ItemInHandRendererMixin.java` - the parameter list of `renderHandsWithItems`. If the game crashes at launch
  with `InvalidInjectionException`, adjust the parameters to what the error message lists.
* `KnifeViewRenderer.java` - `RenderType.entityCutoutNoCull(...)` and `SubmitNodeCollector.submitCustomGeometry(...)`.
* If your mappings call `ResourceLocation` -> `Identifier`: search & replace in `Ids.java`, `KnifeRegistry.java`,
  `KnifeController.java`.
* Menu drawing uses `GuiGraphics.fill / drawString / blit(RenderPipelines.GUI_TEXTURED, ...)`.
Send me the compiler / crash output and I'll fix it.

## Known limits
* No first-person arms/hands yet (just the knife).
* While active, vanilla off-hand rendering is hidden too.
* Visuals are client-only: other players do not see your knife.
* DonutSMP: nothing is sent to the server and no server mod is needed, but I can't verify their rules about
  client mods. Check their rules / ask staff before using it there.

## Regenerating assets
`python3 tools/convert_assets.py cs2knives-1_7_0-fabric-1_21_11.jar src/main/resources`
(converts the TACZ models/animations to compact `.bin` files; `tools/preview*.py` are debug renderers).
