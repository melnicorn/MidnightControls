# Porting MidnightControls to Minecraft 26.3 (Fabric)

This document is the complete record of the 26.3 port so that it can be contributed upstream
(https://github.com/TeamMidnightDust/MidnightControls, branch `multiversion`). It covers every remap,
every structural change, every assumption that could not be verified in the porting environment, and the
in-game test plan.

## 1. Summary

Minecraft 26.3 replaced GLFW with SDL3 (`lwjgl-sdl:3.4.3`; `lwjgl-glfw` is no longer on the client classpath).
MidnightControls used GLFW directly for gamepads, cursor control and a few key/mouse constants (~350 references
in 18 files). The port:

1. adds a `26.3-fabric` Stonecutter node (Fabric only, no NeoForge node);
2. introduces a small platform abstraction (`InputBackend`) with a GLFW implementation (<= 26.2) and an SDL3
   implementation (>= 26.3), selected at build time with Stonecutter;
3. replaces every GLFW gamepad/joystick constant with mod-local constants (`GamepadConstants`) that keep GLFW's
   numbering, so saved configs and the binding/rendering code are untouched;
4. replaces the handful of synthesised keyboard/mouse codes with `KeyCodes` (GLFW key codes before 26.3, SDL
   scancodes from 26.3);
5. gates the 26.3 signature changes of `MouseHandler` and two fragile injection points.

Everything up to 26.2 is a pure refactor: the GLFW backend performs exactly the same native calls as before.

## 2. Build setup

| File | Change |
|------|--------|
| `settings.gradle.kts` | `mc("fabric", "1.21.11", "26.1", "26.2", "26.3")` (NeoForge list unchanged) |
| `versions/26.3-fabric/gradle.properties` | new node; see below |

```properties
mod.mc_dep_fabric=>=26.3
mod.mc_dep_forgelike=[26.3,)
mod.mc_title=26.3
mod.mc_targets=26.3
deps.forge_loader=0
deps.neoforge_loader=0
deps.fabric_loader=0.19.5        # root gradle.properties pins 0.18.2, which predates the SDL client
deps.fabric_version=0.161.0+26.3
deps.midnightlib_version=1.9.3   # resolves to 1.9.3+26.3-fabric
deps.modmenu_version=20.0.1
deps.sodium_version=mc26.3-0.9.2
deps.spruceui_version=12.0.0+26.3
deps.yumimc_version=1.1.3+26.2   # pulled transitively by SpruceUI 12.0.0+26.3
deps.yacl_version=3.9.7          # resolves to 3.9.7+26.3-fabric
deps.emotecraft_version=3.4.0-b.build.161
deps.libgui_version=18.0.1+26.3-rc-2
loom.platform=fabric
```

`stonecutter.gradle.kts` keeps `26.2-fabric` active. Build the new node with
`./gradlew :26.3-fabric:build --console=plain` (Java 25 required).

## 3. Architecture: `InputBackend`

Package `eu.midnightdust.midnightcontrols.client.controller.backend`:

| Class | Role |
|-------|------|
| `InputBackend` | interface: connection/gamepad queries per slot, `readState`, mappings, error polling, connection listener, Wayland detection, cursor mode and cursor warp |
| `InputBackends` | holds the instance; `//? if >=26.3` picks `SdlInputBackend`, else `GlfwInputBackend` |
| `GamepadState` | replaces `GLFWGamepadState`: `boolean[19] buttons`, `float[6] axes` in GLFW conventions |
| `GlfwInputBackend` | whole file gated `//? if <26.3`; same GLFW calls the mod made before |
| `SdlInputBackend` | whole file gated `//? if >=26.3`; see section 4 |

`eu.midnightdust.midnightcontrols.client.controller.GamepadConstants` holds the button/axis/slot ids.
`eu.midnightdust.midnightcontrols.client.util.KeyCodes` holds the key/mouse codes.
`eu.midnightdust.midnightcontrols.client.util.MouseUtil#onCursorPos` wraps the version-specific
`MouseHandler#onMove` invoker.

Whole-file Stonecutter gating requires that the file contains no nested block comments; therefore the two backend
files and `KeyCodes` use `//` comments only.

## 4. SDL3 backend details

* **Initialisation.** `SDL_InitSubSystem(SDL_INIT_GAMEPAD)` is called from `MidnightControlsClient.onMcInit`
  (end of `Minecraft.<init>`, main thread, window exists). It is skipped if the game already initialised it.
* **State polling.** Minecraft's event loop calls `SDL_PumpEvents` every frame, which updates joystick state; the
  backend additionally calls `SDL_UpdateGamepads()` once per client tick. State is read with
  `SDL_GetGamepadButton` / `SDL_GetGamepadAxis` under `SDL_LockJoysticks`, which is safe from the 1 kHz camera
  thread (the GLFW code had the same threading pattern).
* **Slots.** SDL uses ever-increasing instance ids; the mod, its config (`controllerID`, `secondControllerID`)
  and the settings screen expect GLFW-style ids `0..15`. The backend keeps a 16-entry slot table: each joystick
  gets the lowest free slot when it appears and frees it when it disappears.
* **Hot-plug.** `SDL_GetJoysticks()` is re-enumerated every client tick (the game's `SDLEventHandler` swallows
  `SDL_EVENT_GAMEPAD_ADDED/REMOVED`, so an event watch was not used). Connect/disconnect toasts and
  `switchControlsMode()` are driven from that scan through the same listener the GLFW joystick callback used.
  Devices present at start-up are registered silently, matching GLFW behaviour.
* **Gamepad vs joystick.** A slot is "connected" if the joystick exists and "gamepad" if `SDL_IsGamepad` is true
  and `SDL_OpenGamepad` succeeded. Because mapping files load asynchronously, the per-tick scan retries opening
  slots that were not gamepads yet.
* **Mappings.** `glfwUpdateGamepadMappings(buffer)` became `SDL_AddGamepadMappingsFromIO(SDL_IOFromConstMem(buffer), true)`.
  The format (SDL_GameControllerDB) is identical; the trailing NUL the mod appends for GLFW is stripped.
  Errors surface through `SDL_GetError()` in the same toast as before.
* **GUIDs.** `SDL_GetJoystickGUIDForID` + `SDL_GUIDToString`. Note that SDL3 GUID strings are not byte-identical
  to GLFW's on every platform (SDL encodes bus type and a name CRC in the first bytes), so a controller profile
  saved under 26.2 may not match and the mod falls back to slot 0 / default bindings. One-time re-selection in the
  settings screen fixes it.
* **Names.** `SDL_GetGamepadNameForID` (falls back to `SDL_GetJoystickNameForID`). `matchControllerToType()`
  matches on substrings like "xbox", "playstation"/"dualsense", so PS5 icons should still resolve.
* **Wayland.** `glfwGetVersionString().contains("Wayland")` became `"wayland".equalsIgnoreCase(SDL_GetCurrentVideoDriver())`.
  It is evaluated again in `onMcInit` because the video driver is not initialised when Fabric entrypoints run.

### 4.1 Button mapping (mod/GLFW id <-> SDL3 `SDL_GamepadButton`)

| Mod id | GLFW name | SDL3 constant | Note |
|-------:|-----------|---------------|------|
| 0 | GAMEPAD_BUTTON_A | SDL_GAMEPAD_BUTTON_SOUTH (0) | Cross on PS5 |
| 1 | GAMEPAD_BUTTON_B | SDL_GAMEPAD_BUTTON_EAST (1) | Circle |
| 2 | GAMEPAD_BUTTON_X | SDL_GAMEPAD_BUTTON_WEST (2) | Square |
| 3 | GAMEPAD_BUTTON_Y | SDL_GAMEPAD_BUTTON_NORTH (3) | Triangle |
| 4 | GAMEPAD_BUTTON_LEFT_BUMPER | SDL_GAMEPAD_BUTTON_LEFT_SHOULDER (9) | |
| 5 | GAMEPAD_BUTTON_RIGHT_BUMPER | SDL_GAMEPAD_BUTTON_RIGHT_SHOULDER (10) | |
| 6 | GAMEPAD_BUTTON_BACK | SDL_GAMEPAD_BUTTON_BACK (4) | Create/Share |
| 7 | GAMEPAD_BUTTON_START | SDL_GAMEPAD_BUTTON_START (6) | Options |
| 8 | GAMEPAD_BUTTON_GUIDE | SDL_GAMEPAD_BUTTON_GUIDE (5) | PS button |
| 9 | GAMEPAD_BUTTON_LEFT_THUMB | SDL_GAMEPAD_BUTTON_LEFT_STICK (7) | L3 |
| 10 | GAMEPAD_BUTTON_RIGHT_THUMB | SDL_GAMEPAD_BUTTON_RIGHT_STICK (8) | R3 |
| 11 | GAMEPAD_BUTTON_DPAD_UP | SDL_GAMEPAD_BUTTON_DPAD_UP (11) | |
| 12 | GAMEPAD_BUTTON_DPAD_RIGHT | SDL_GAMEPAD_BUTTON_DPAD_RIGHT (14) | |
| 13 | GAMEPAD_BUTTON_DPAD_DOWN | SDL_GAMEPAD_BUTTON_DPAD_DOWN (12) | |
| 14 | GAMEPAD_BUTTON_DPAD_LEFT | SDL_GAMEPAD_BUTTON_DPAD_LEFT (13) | |
| 15 | (none, "L4") | SDL_GAMEPAD_BUTTON_LEFT_PADDLE1 (17) | new: paddles were unreachable with GLFW |
| 16 | (none, "L5") | SDL_GAMEPAD_BUTTON_LEFT_PADDLE2 (19) | new |
| 17 | (none, "R4") | SDL_GAMEPAD_BUTTON_RIGHT_PADDLE1 (16) | new |
| 18 | (none, "R5") | SDL_GAMEPAD_BUTTON_RIGHT_PADDLE2 (18) | new |

Ids 15..18 already had icons (`controller_expanded.png`) and translations in the mod; the SDL backend simply
feeds them. `GamepadState.buttons` therefore has 19 entries on every version; the GLFW backend never sets 15..18.
`SDL_GAMEPAD_BUTTON_MISC1` and `TOUCHPAD` are intentionally not exposed (no icon/translation).

### 4.2 Axis mapping and normalisation

| Mod id | GLFW name | SDL3 constant |
|-------:|-----------|---------------|
| 0 | GAMEPAD_AXIS_LEFT_X | SDL_GAMEPAD_AXIS_LEFTX |
| 1 | GAMEPAD_AXIS_LEFT_Y | SDL_GAMEPAD_AXIS_LEFTY |
| 2 | GAMEPAD_AXIS_RIGHT_X | SDL_GAMEPAD_AXIS_RIGHTX |
| 3 | GAMEPAD_AXIS_RIGHT_Y | SDL_GAMEPAD_AXIS_RIGHTY |
| 4 | GAMEPAD_AXIS_LEFT_TRIGGER | SDL_GAMEPAD_AXIS_LEFT_TRIGGER |
| 5 | GAMEPAD_AXIS_RIGHT_TRIGGER | SDL_GAMEPAD_AXIS_RIGHT_TRIGGER |

The numbering is the same; only the value ranges differ. `SdlInputBackend.normalizeAxis` converts:

* sticks: `short -32768..32767` -> `float -1..1` (negative values divided by 32768, positive by 32767);
  Y axes keep "positive = down" in both libraries, no sign flip;
* triggers: `short 0..32767` -> `float -1..1` (`unit * 2 - 1`), because GLFW reports released triggers as `-1`
  and `AxisStorage` / `triggerFix` depend on that. A disconnected or non-gamepad slot reports `-1` for triggers.

### 4.3 Cursor control

| Old call | New call (SDL backend) |
|----------|------------------------|
| `glfwSetInputMode(w, GLFW_CURSOR, GLFW_CURSOR_HIDDEN)` | `SDL_SetWindowRelativeMouseMode(w, false); SDL_HideCursor()` |
| `glfwSetInputMode(w, GLFW_CURSOR, GLFW_CURSOR_DISABLED)` | `SDL_HideCursor(); SDL_SetWindowRelativeMouseMode(w, true)` |
| `glfwSetCursorPos(w, x, y)` | `SDL_WarpMouseInWindow(w, (float) x, (float) y)` |

Call sites: `MidnightControlsClient.onScreenOpen` (hideNormalMouse), `MouseMixin.midnightcontrols$lockCursor`
(mixed input / eye tracker), `InputManager.updateMousePosition` (hardware cursor follows the virtual one).

## 5. Keyboard and mouse code remaps (`KeyCodes`)

The mod synthesises these keys for GUI navigation (`pressKeyboardKey`, slider nudging, chat send, WASD screens)
and for its own key mappings (numpad look keys):

| Use | GLFW code (<= 26.2) | SDL scancode (>= 26.3) |
|-----|--------------------:|-----------------------:|
| Enter (`ENTER_KEY_INPUT`, `handleAButton`, chat send) | `GLFW_KEY_ENTER` 257 | `SDL_SCANCODE_RETURN` 40 |
| Up / Down / Left / Right (`changeFocus`) | 265 / 264 / 263 / 262 | 82 / 81 / 80 / 79 |
| Left / Right (slider nudge, was hard-coded `263`/`262`) | 263 / 262 | 80 / 79 |
| W / S / A / D (`wasdScreens`) | 87 / 83 / 65 / 68 | 26 / 22 / 4 / 7 |
| Numpad 8 / 6 / 2 / 4 (`BINDING_LOOK_*` defaults) | 328 / 326 / 322 / 324 | 96 / 94 / 90 / 92 |

Mouse buttons and actions:

| Use | <= 26.2 | >= 26.3 |
|-----|---------|---------|
| Left click (A button, hotbar/touch, inventory actions) | `GLFW_MOUSE_BUTTON_1` (0) | `InputConstants.MOUSE_BUTTON_LEFT` |
| Right click (X button, "take") | `GLFW_MOUSE_BUTTON_2` (1) | `InputConstants.MOUSE_BUTTON_RIGHT` |
| Back thumb button (`tryGoBack`) | `GLFW_MOUSE_BUTTON_4` (3) | `InputConstants.MOUSE_BUTTON_LEFT + 3` (GLFW 0->3, SDL 1->4; see note below) |
| Press / release action ints | `GLFW_PRESS` 1 / `GLFW_RELEASE` 0 | `InputConstants.PRESS` / `InputConstants.RELEASE` |

Note on the back button: whether 26.3 keeps GLFW's 0-based or SDL's 1-based button numbering inside
`MouseButtonInfo` is unknown; deriving it from `MOUSE_BUTTON_LEFT` gives the right id in both schemes.

The virtual keyboard (`virtualkeyboard/`, `assets/midnightcontrols/keyboard_layouts/*.json`) stores characters,
not key codes, and needed no change.

## 6. Mixin changes

| Mixin | Change |
|-------|--------|
| `MouseAccessor` | `onMove` invoker is `(long, double, double)` before 26.3 and `(long, double, double, double, double)` from 26.3 (absolute position plus relative motion). Callers go through `MouseUtil.onCursorPos`, which derives the deltas from the previous `xpos()/ypos()`. |
| `MouseMixin` | cursor hiding via the backend; the `grabMouse` injection targets `InputConstants.grabOrReleaseMouse` by name only and with `require = 0` on 26.3 (its GLFW int descriptor is gone). |
| `InputUtilMixin` | `isRawMouseInputSupported` injection gets `require = 0` on 26.3 (raw mouse input is a GLFW concept). |

Mixins that were **not** changed and whose 26.3 targets could not be verified here: `KeyboardAccessor`
(`keyPress(long, int, KeyEvent)`), `KeyboardMixin`, `CursorMixin` (`CursorType.select(Window)`), the `@Shadow`
fields of `MouseMixin` (`xpos`, `ypos`, `accumulatedDX/DY`, `mouseGrabbed`, `ignoreFirstMove`, `mousePressedTime`,
`smoothTurnX/Y`, `isLeftPressed`), `MinecraftClientMixin`, `WorldRendererMixin`, `GuiMixin`, `GameRendererMixin`,
`InGameHudMixin`, and the Sodium compat accessor. If 26.3 renamed any of them the game reports the exact member at
start-up; each is a one-line fix with a `//? if >=26.3` gate.

## 7. Verification status (read this before testing)

The porting environment had no network route to Mojang's, Fabric's or any mod Maven, so **no Gradle build of any
node could be run here** (`piston-meta.mojang.com`, `libraries.minecraft.net`, `maven.fabricmc.net`,
`maven.midnightdust.eu`, `api.modrinth.com`, etc. were all denied by policy). What was verified:

* The new/gated sources (`GamepadConstants`, `GamepadState`, `InputBackend`, `InputBackends`,
  `GlfwInputBackend`, `SdlInputBackend`, `KeyCodes`) were preprocessed for both 26.2 and 26.3 and compiled with
  JDK 25 (`-Xlint:all`, no warnings) against the real `lwjgl-3.4.3`, `lwjgl-sdl-3.4.3` and `lwjgl-glfw-3.4.3`
  jars. Every SDL/GLFW function and constant used therefore exists with the used signature.
* All other edits are constant renames and call-site rewrites with no change in semantics; the remaining code
  does not reference `org.lwjgl.glfw` anywhere outside the GLFW backend (checked with grep).

Assumptions taken from the task description that could not be checked against the 26.3 jar:

1. `Window.handle()` returns the `SDL_Window*` pointer (used for warp/relative-mouse calls and for the
   `window != handle()` check in `MouseMixin`).
2. `MouseHandler.onMove(long, double, double, double, double)` is `(window, x, y, xrel, yrel)`.
3. `MouseHandler.onButton(long, MouseButtonInfo, int)` keeps the `1 = press, 0 = release` action ints.
4. `KeyEvent.key()` carries SDL scancodes and `KeyboardHandler.keyPress(long, int, KeyEvent)` is unchanged.
5. `InputConstants.MOUSE_BUTTON_LEFT/RIGHT`, `PRESS`, `RELEASE` still exist (they are used by `Options`).
6. Mouse coordinates handed to `onMove` / `SDL_WarpMouseInWindow` are in the same space as
   `MouseHandler.xpos()` (window points). On Retina displays verify the virtual cursor lands where it is drawn.

## 8. Files changed

New: `GamepadConstants`, `backend/{GamepadState,InputBackend,InputBackends,GlfwInputBackend,SdlInputBackend}`,
`util/KeyCodes`, `util/MouseUtil`, `versions/26.3-fabric/gradle.properties`, this document.

Modified: `settings.gradle.kts`, `MidnightControlsClient`, `MidnightControlsConfig`, `MidnightInput`,
`controller/{Controller,InputManager,ButtonBinding,InputHandlers}`, `compat/{EmotecraftCompat,EMICompat}`,
`gui/{MidnightControlsRenderer,MidnightControlsSettingsScreen}`, `gui/config/ControllerSelectionButton`,
`gui/widget/ControlsListWidget`, `mixin/{MouseAccessor,MouseMixin,InputUtilMixin}`,
`touch/gui/TouchscreenOverlay`, `util/storage/{AxisStorage,ButtonStorage}`,
`virtualkeyboard/clickhandler/DefaultScreenClickHandler`, `assets/midnightcontrols/lang/en_us.json`
(R5 label typo), `CHANGELOG.md`.

Nothing was stubbed out or disabled; the two `require = 0` injections degrade only the eye-tracker / mixed-input
cursor behaviour if their targets are missing.

## 9. In-game test checklist (26.3, macOS, PS5 DualSense)

Start-up
- [ ] Log shows `Using SDL3 input backend.` and `[SDL3] Gamepad subsystem initialized (video driver: cocoa).`
- [ ] No mixin apply errors / `InjectionError` in the log; title screen reachable.
- [ ] Log shows `[SDL3] Loaded N gamepad mapping(s).` after the controller DB download (needs internet).

Detection and hot-plug
- [ ] Controller connected before launch is listed in Settings > Controller with its name (not red/gold).
- [ ] Plugging in / Bluetooth-pairing after launch shows the "Controller N connected" toast and auto-switches
      to controller mode (if auto switch is on); unplugging shows the disconnect toast and switches back.
- [ ] Re-plugging reuses slot 0 (settings keep pointing at the same controller).
- [ ] Controller type auto-detection picks PlayStation icons; manual type override still works.

Sticks, triggers, buttons
- [ ] Left stick walks in all directions with analog speed; deadzone sliders apply.
- [ ] Right stick looks around; both camera modes (adaptive/flat); invert options.
- [ ] L2/R2 attack/use: light pull does nothing below the trigger deadzone, full pull acts; `triggerFix` off.
- [ ] L1/R1 hotbar, D-pad actions, L3 sprint, R3 sneak, Options pauses, PS button opens the ring, Create shows
      the player list.
- [ ] Button remapping screen records single buttons, chords, stick directions and triggers; saved bindings
      survive a restart (config `controller.controls.*` values are the same integers as on 26.2).
- [ ] If the controller has back paddles (DualSense Edge / Xbox Elite), L4/L5/R4/R5 can be bound.

Menus / virtual cursor
- [ ] D-pad / left stick move focus in menus; Cross activates, Circle goes back, sliders nudge with left/right.
- [ ] Inventory: virtual cursor moves with the left stick and snaps to slots; Cross/Square/Triangle = take /
      take-one / quick move; L1/R1 switch tabs in creative; right stick scrolls lists.
- [ ] With `virtualMouse` off, the hardware cursor follows the stick (uses `SDL_WarpMouseInWindow`).
- [ ] `hideNormalMouse` hides the OS cursor in menus and re-grabs it in game.
- [ ] Chat: Triangle opens, virtual keyboard types, done sends the message (Enter scancode).
- [ ] Mouse "back" thumb button goes back in menus (verifies the `MOUSE_BUTTON_BACK` derivation).

Touch / mixed input
- [ ] Touchscreen mode overlay renders and look-by-drag works (uses axis ids 2/3 only, no SDL calls).
- [ ] Mixed input (mouse + controller) keeps the cursor hidden but free; eye-tracker mode toggles without a crash.

Other versions
- [ ] `./gradlew :26.2-fabric:build` and `:1.21.11-fabric:build` still pass (GLFW backend path).

## 10. Known gaps / follow-ups

* SDL GUIDs differ from GLFW GUIDs on some platforms; per-controller binding profiles keyed by GUID need
  re-selection once after upgrading.
* The SDL backend does not use rumble, touchpad or gyro yet (`SDL_RumbleGamepad`, touchpad events,
  `SDL_GetGamepadSensorData` are available if wanted).
* `MidnightControlsConfig.getController()` iterates `i < JOYSTICK_LAST` (skips slot 15); pre-existing, unchanged.
