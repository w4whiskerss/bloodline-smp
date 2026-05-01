# BloodLine Client

Fabric client mod for BloodLine SMP.

Features:
- hidden hotkey networking through plugin messages
- custom bloodline HUD above the hotbar
- custom popup box for ability feedback
- cooldown and timer rendering
- OmniBlade HUD override when held
- rebindable hotkeys through the Controls menu

Default hotkeys:
- `V` -> Primary
- `B` -> Secondary
- `N` -> Special

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
- `client-mod/build/libs/BloodLine-Client-2.3.4.jar`

Install:
1. Install Fabric for Minecraft `1.21.11`.
2. Put the built mod jar in your client `mods` folder.
3. Put the matching Fabric API jar in the same `mods` folder.
4. Join the server with `BloodLine Server 2.3.4` installed.
