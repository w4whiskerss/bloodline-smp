<div align="center">

<img width="100%" alt="Bloodline SMP Banner" src="https://capsule-render.vercel.app/api?type=waving&height=220&color=0:111827,20:1E3A8A,55:C2410C,100:6D28D9&text=Bloodline%20SMP&fontSize=56&fontColor=ffffff&animation=fadeIn&fontAlignY=38&desc=PaperMC%20PvP%20Bloodline%20System&descAlignY=60&descSize=20" />

<br />

[![repo](https://img.shields.io/badge/repo-bloodlinesmp-0f172a?style=flat-square)](https://github.com/w4whiskerss/bloodline-smp)
[![live version](https://img.shields.io/badge/version-2.0.17-f97316?style=flat-square)](https://github.com/w4whiskerss/bloodline-smp/releases)
[![paper](https://img.shields.io/badge/paper-1.21.11-2563eb?style=flat-square)](https://papermc.io/)
[![java](https://img.shields.io/badge/java-21-22c55e?style=flat-square)](https://adoptium.net/)
[![focus](https://img.shields.io/badge/focus-bloodline%20combat-a855f7?style=flat-square)](https://github.com/w4whiskerss/bloodline-smp)
[![style](https://img.shields.io/badge/style-public%20smp%20ready-06b6d4?style=flat-square)](https://github.com/w4whiskerss/bloodline-smp)

<br />

[![YouTube](https://img.shields.io/badge/YouTube-W4Whiskerss-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@W4Whiskerss)
[![Discord](https://img.shields.io/badge/Discord-Private%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/NjZg46TRmf)
[![GitHub](https://img.shields.io/badge/GitHub-bloodline--smp-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/w4whiskerss/bloodline-smp)

<br />
<br />

Custom **PaperMC PvP plugin** for **Minecraft 1.21.11** built around bloodlines, progression, chaos, and creator-style SMP gameplay.

Bloodline SMP gives each player **one active bloodline**, custom passives, ability scaling up to **Level 5**, PvP drops, admin tools, grace period controls, and an optional client hotkey mod.

</div>

---

## Overview

Bloodline SMP is a full custom **Paper plugin** built for **Minecraft 1.21.11** with **Java 21**.

Players are assigned one bloodline and fight, level up, steal progress, switch through trait potions, and unlock high-impact abilities built around PvP gameplay.

This repo also includes an optional **Fabric client hotkey mod** for custom ability binds:

- `Shift + V` -> Primary
- `Shift + B` -> Secondary
- `Shift + N` -> Special

---

## Main Features

- 4 main bloodlines: `Aqua`, `Spartan`, `Earthian`, `Voider`
- Unique passives, primary, secondary, and special abilities
- Bloodline levels from `1` to `5`
- First-join weighted bloodline selection
- Trait potions and upgrade potions from PvP deaths
- Admin GUI for editing player bloodlines and levels
- Grace period system with boss bar countdown
- Discord webhook support
- Optional custom client hotkey mod

---

## Bloodline Rule

Players can only have **one active bloodline at a time**.

If a player switches bloodlines, the old active bloodline is replaced.  
They do **not** get free switching back.

To return to an old bloodline, they must obtain that bloodline's **Trait Potion** again.

Example:

- Switch from `Aqua` to `Spartan` -> Aqua is no longer your active bloodline
- Want Aqua back later -> get an `Aqua Trait Potion`
- Want Spartan again after that -> get a `Spartan Trait Potion`

---

## Bloodlines

### Aqua
- Water-based mobility and control
- Water buffs while submerged
- Water Dash
- Suffocation Curse
- Tidal Surge

### Spartan
- Fire-based aggression
- Permanent Fire Resistance
- Fireball
- Flaming Hands
- Hell Dominion

### Earthian
- Tank and area-control bloodline
- Resistance and anti-knockback passives
- Ground Slam
- Root Prison
- Worldbreaker

### Voider
- Mobility and chaos bloodline
- Random daily passive buff
- Void Blink
- Void Send
- Void Flight

Full Discord-ready ability write-up:
[docs/discord-bloodline-info.md](docs/discord-bloodline-info.md)

---

## Controls

### Default server controls
- `Shift + Right Click` -> Primary
- `Shift + Left Click` -> Secondary
- `Shift + F` -> Special

### Optional client mod controls
- `Shift + V` -> Primary
- `Shift + B` -> Secondary
- `Shift + N` -> Special

### Command fallback
- `/ability1`
- `/ability2`
- `/ability3`

---

## Commands

### Player
- `/bloodline`
- `/ability1`
- `/ability2`
- `/ability3`

### Admin
- `/bloodlineadmin`
- `/bloodlinereload`
- `/gracestart`
- `/gracestop`
- `/gracetimeset <duration>`
- `/bloodlinetest ...`

---

## Build

### Server plugin

```powershell
.\gradlew.bat jar
```

Output:

- `build/libs/bloodline-smp-2.0.17.jar`

### Local test server

```powershell
.\gradlew.bat runServer
```

### Client mod

```powershell
cmd /c gradlew.bat -p client-mod build
```

Output:

- `client-mod/build/libs/bloodline-hotkeys-1.0.1.jar`

---

## Config

Generated config path on the server:

- `plugins/BloodlineSMP/config.yml`

Includes settings for:

- grace period duration
- test command toggle
- admin panel usernames
- first-join bloodline rates
- Discord webhook
- all bloodline cooldowns and scaling

---

## Links

- YouTube: [@W4Whiskerss](https://www.youtube.com/@W4Whiskerss)
- Discord: [Private Server](https://discord.gg/NjZg46TRmf)
- GitHub: [bloodline-smp](https://github.com/w4whiskerss/bloodline-smp)

---

## Credits

Created by **w4whiskers**.
