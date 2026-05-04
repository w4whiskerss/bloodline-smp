<div align="center">

<img width="100%" alt="Bloodline SMP Banner" src="https://capsule-render.vercel.app/api?type=waving&height=220&color=0:120608,25:3b0a0f,55:7f1d1d,80:b91c1c,100:450a0a&text=Bloodline%20SMP&fontSize=56&fontColor=fff7f7&animation=fadeIn&fontAlignY=38&desc=PaperMC%20PvP%20Bloodline%20System&descAlignY=60&descSize=20" />

[![version](https://img.shields.io/badge/version-2.3.6-dc2626?style=flat-square)](https://github.com/w4whiskerss/bloodline-smp/releases)
[![paper](https://img.shields.io/badge/paper-1.21.11-7f1d1d?style=flat-square)](https://papermc.io/)
[![java](https://img.shields.io/badge/java-21-991b1b?style=flat-square)](https://adoptium.net/)
[![fabric](https://img.shields.io/badge/client%20mod-fabric-b91c1c?style=flat-square)](https://fabricmc.net/)

[![YouTube](https://img.shields.io/badge/YouTube-W4Whiskerss-FF0000?style=for-the-badge&logo=youtube&logoColor=white)](https://www.youtube.com/@W4Whiskerss)
[![Discord](https://img.shields.io/badge/Discord-Private%20Server-5865F2?style=for-the-badge&logo=discord&logoColor=white)](https://discord.gg/NjZg46TRmf)

Custom PvP bloodline system for Minecraft SMP content, built as a Paper plugin with an optional Fabric client HUD mod.

</div>

## What This Repo Contains

This repo includes both sides of the project:

- A `Paper 1.21.11` server plugin
- A matching optional `Fabric` client mod
- Ability textures and local art assets used for HUD/icon work

The server plugin handles bloodlines, progression, commands, balancing, world rules, grace periods, drops, and admin tools.

The client mod adds:

- hotkey-based ability activation
- custom HUD boxes with icons
- cooldown overlays and timer bars
- popup feedback
- OmniBlade override support
- rebindable keybinds

## Core Gameplay

Each player has one active bloodline. Bloodlines level from `1` to `5` and unlock stronger passives and abilities over time.

Current main bloodlines:

- `Aqua`
- `Spartan`
- `Earthian`
- `Voider`

Gameplay supports:

- first-join bloodline rolling
- bloodline upgrades and switching
- trait potion progression
- PvP-driven rewards and punishments
- scripted and public balance modes
- disabled worlds where bloodline gameplay can be shut off

## Bloodline Rules

Players can only use one active bloodline at a time.

If a player switches bloodlines, the old one is replaced as the active bloodline. To get an old bloodline back later, they need to obtain that bloodline's trait item again.

## Ability Layout

Bloodlines can expose different ability counts depending on the power:

- some bloodlines use `3` core abilities
- some bloodlines unlock `4th` and `5th` abilities at higher levels
- OmniBlade override can expose all `5` slots

The client HUD is built to reflect the active bloodline's actual slot count instead of always showing five.

## Default Controls

Server-side controls:

- `Shift + Right Click` -> Primary
- `Shift + Left Click` -> Secondary
- `Shift + F` -> Special

Optional client mod defaults:

- `V` -> Ability 1
- `B` -> Ability 2
- `N` -> Ability 3
- `G` -> Ability 4
- `H` -> Ability 5

Command fallbacks:

- `/ability1`
- `/ability2`
- `/ability3`
- `/ability4`
- `/ability5`

## Commands

Player-facing:

- `/bloodline`
- `/ability1`
- `/ability2`
- `/ability3`
- `/ability4`
- `/ability5`

Admin / testing:

- `/bloodlineadmin`
- `/bloodlinereload`
- `/gracestart`
- `/gracestop`
- `/gracetimeset <duration>`
- `/bloodlinetest ...`

Disabled worlds are intended to block bloodline gameplay abilities, not general admin or utility command access.

## Included Systems

- bloodline GUI and admin GUI
- grace period boss bar and timers
- public-SMP balance tuning
- world-specific bloodline disabling
- client/server handshake logic
- client HUD sync packets
- cooldown tracking
- Discord webhook support

## Project Structure

Important paths:

- `src/main/java/...` -> server plugin source
- `client-mod/src/main/java/...` -> Fabric client mod source
- `client-mod/src/main/resources/assets/...` -> HUD icons, lang, and client resources
- `textures/` -> raw/source texture references

## Building

Server plugin build:

```powershell
.\gradlew.bat build
```

Client mod build:

```powershell
cd client-mod
..\gradlew.bat build
```

Outputs:

- `build/libs/BloodLine Server-<version>.jar`
- `client-mod/build/libs/BloodLine-Client-<version>.jar`

## Requirements

- Java `21`
- Paper `1.21.11`
- Fabric client only if you want the optional custom HUD/hotkeys

## Links

- YouTube: [@W4Whiskerss](https://www.youtube.com/@W4Whiskerss)
- Discord: [Private Server](https://discord.gg/NjZg46TRmf)

## Credits

Created by **w4whiskers**.


