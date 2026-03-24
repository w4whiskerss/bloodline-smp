# Bloodline Hotkeys

Small Fabric client mod for BloodlineSMP.

Default hotkeys:
- `Shift + V` -> `/ability1`
- `Shift + B` -> `/ability2`
- `Shift + N` -> `/ability3`

Requirements:
- Minecraft `1.21.11`
- Fabric Loader `0.18.1`
- Fabric API `0.141.1+1.21.11`
- Java `21`

Build:
```powershell
cd client-mod
..\gradlew.bat -p client-mod build
```

Output:
- `client-mod/build/libs/bloodline-hotkeys-1.0.1.jar`

Install:
1. Install Fabric for Minecraft `1.21.11`.
2. Put the built mod jar in your client `mods` folder.
3. Put the matching Fabric API jar in the same `mods` folder.
4. Join the server with `bloodline-smp-2.0.11.jar` installed.
