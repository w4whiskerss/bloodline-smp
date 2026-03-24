<div align="center">

# Bloodline SMP

### A custom PaperMC PvP bloodline system for Minecraft 1.21.11

[![Version](https://img.shields.io/badge/version-2.0.16-111111?style=for-the-badge&logo=github)](https://github.com/w4whiskerss/bloodline-smp)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-00C853?style=for-the-badge&logo=minecraft)](https://papermc.io/)
[![Java](https://img.shields.io/badge/Java-21-F89820?style=for-the-badge&logo=openjdk)](https://adoptium.net/)

[![YouTube](https://img.shields.io/badge/YouTube-W4Whiskerss-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@W4Whiskerss)
[![Discord](https://img.shields.io/badge/Discord-Private%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/NjZg46TRmf)
[![Repo](https://img.shields.io/badge/GitHub-bloodlinesmp-181717?style=for-the-badge&logo=github&logoColor=white)](https://github.com/w4whiskerss/bloodline-smp)

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

- `build/libs/bloodline-smp-2.0.16.jar`

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
