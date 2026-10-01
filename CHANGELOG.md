### MidnightControls v1.13.0 (unreleased)
- Port to Minecraft 26.3 (Fabric only)
  - Minecraft 26.3 replaced GLFW with SDL3, so controller input, cursor control and controller mappings now go
    through a small platform abstraction (`InputBackend`) with GLFW (<= 26.2) and SDL3 (>= 26.3) implementations
  - Saved controller bindings keep their numeric ids; nothing needs to be re-bound
  - Controllers with back paddles can now bind L4/L5/R4/R5 (SDL3 backend only)
  - See `docs/PORTING-26.3.md` for the full porting record and test checklist
- Fix the R5 button label

### MidnightControls v1.12.1
- Port to 26.1
- Compatibility with certain mods needed to temporarily disabled until they're updated

## MidnightControls v1.12.0
- Re-enable **NeoForge** support!
- Switch to stonecutter build system
  - Eases the process of maintaining the mod for older versions – backports incoming :)
- Migrate to official mappings (mojmap)
  - This reduces compile times and makes the mod future-proof
- Analog movement is now handled in a better way
  - When joining servers that are known to have anti-cheat systems, analog movement will now be disabled to avoid laggy movement and potential bans. Server IPs can be added in the config.
  - Non-analog movement no longer negatively affects joystick movement in GUIs
- Fix controller inputs not resetting the idle timer
- Fix sneak movement being too slow
- Improved code quality and maintainability