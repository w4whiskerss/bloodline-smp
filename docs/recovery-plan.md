# BloodLine SMP Recovery Plan

This document captures the current state of the recovered snapshot and the fastest path back to the last known `2.3.4` feature set.

## Snapshot Summary

This workspace is not a clean Git clone. It appears to be a later source snapshot with mixed project ages:

- Server metadata is on `2.3.4`
- Main README is much older and still references `2.0.18`
- Client mod docs and implementation are older hotkey-only versions
- Some newer server gameplay systems already exist

That means recovery should be treated as a selective rebuild, not a full rewrite.

## Confirmed Present

### Server core

- Paper plugin structure is intact
- Bloodline progression and active bloodline ownership exist
- Trait potion and upgrade progression systems exist
- Omni / Universal systems exist in server code
- OmniBlade item and recipe exist
- Grace period system exists
- Admin GUI exists
- Persistent player data exists
- Cooldown persistence exists

### Bloodlines and abilities

- Aqua base set exists
- Spartan base set exists
- Earthian base set exists
- Voider base set exists
- Universal bloodline exists

### Already newer than the old README

- `plugin.yml` and Gradle are on `2.3.4`
- Earthian uses `root-trap` naming in config
- Safer `Void Blink` behavior appears present
- Void Flight boss bar timer exists
- OmniBlade logic is already implemented

## Confirmed Missing Or Still Old

### Config and server modes

- No `scripted` / `public` mode system found
- No `public-smp` / `public_smp` alias handling found
- No disabled-world support found
- No `admin-mode` config toggle found
- No richer Discord mode config found

### Admin and debug tooling

- No later debug tab found
- No zero-cooldown toggle found
- No restore-normal-cooldowns action found
- No clear-cooldowns action found

### Client mod and client UX

- Client mod still sends chat commands for handshake and abilities
- No hidden plugin-message hotkey networking found
- No custom bloodline HUD found
- No locked/unlocked icon HUD found
- No cooldown sweep overlays found
- No rebindable hotkeys found
- No custom popup box replacement for action bar found
- No OmniBlade HUD override found
- No client-side custom timer bars found

### Docs and release polish

- README is outdated
- Client README is outdated
- Jar naming is not at the final `BloodLine Server 2.3.4` / `BloodLine Client 2.3.4` state
- Wiki / docs refresh is not present in this snapshot

## Likely Partial Or Needs Verification During Rebuild

These features may exist in some form and should be verified while rebuilding:

- Entity support for all later-updated abilities
- Public-mode replacements for older domain-style abilities
- Earthian Consume sink/bury version
- Obsidian Cage timed structure with timer UI
- Spartan anti-water-extinguish eternal flame behavior
- Client requirement timeout improvements

## Rebuild Order

Restore features in this order so server and client work stay aligned.

### Phase 1: Config and feature flags

Goal: restore the switches that control behavior before changing ability logic.

Tasks:

- Add gameplay mode config with `scripted` and `public`
- Accept `public-smp` and `public_smp` as public aliases
- Add disabled-world support
- Add admin/test command enable flags
- Add `admin-mode` toggle behavior if still required
- Expand Discord config to support:
  - `mode: webhook`
  - `mode: bot`
  - `url`
  - `bot-token`
  - `channel-id`

### Phase 2: Server-side gameplay parity

Goal: restore behavior differences that affect actual SMP play.

Tasks:

- Implement public-mode ability replacements
- Re-check bloodline switching reset rules against remembered behavior
- Add world gating for:
  - abilities
  - bloodline items
  - passive effects
  - admin/test actions if needed
- Rebuild later ability reworks that are truly missing:
  - Earthian Consume
  - Obsidian Cage
  - public Void Collapse / old scripted domain variant
  - public Inferno Rush / old scripted domain variant
- Verify entity support across expanded abilities

### Phase 3: Admin and debug recovery

Goal: restore fast operator tooling for testing and moderation.

Tasks:

- Add debug tab to admin panel
- Add zero-cooldown mode
- Add restore-normal-cooldowns action
- Add clear-cooldowns action
- Re-add item/test tools that are missing

### Phase 4: Networking and handshake rewrite

Goal: stop console/chat spam and support richer client sync.

Tasks:

- Replace `/bloodlinemod` chat-command handshake with hidden plugin messaging
- Replace `/ability1`, `/ability2`, `/ability3` hotkey sending with hidden packets
- Rebuild client-required handshake enforcement
- Rebuild handshake timeout handling so players do not need multiple joins
- Define a clean server-to-client sync payload for:
  - active bloodline
  - level
  - ability unlock state
  - cooldown state
  - OmniBlade override state
  - custom timers

### Phase 5: Client HUD rebuild

Goal: restore the user-facing client polish after the network contract is stable.

Tasks:

- Add custom HUD above hotbar
- Add locked/unlocked ability icons
- Add cooldown sweep overlays
- Add rebindable hotkeys
- Add popup message box to replace action bar spam
- Add custom timer bars for cage / flight / similar mechanics
- Add OmniBlade 5-slot override HUD

### Phase 6: Docs and release cleanup

Goal: bring the repo back to a coherent release state.

Tasks:

- Update root README
- Update client README
- Add changelog notes for recovered features
- Update docs to reflect final config layout
- Rename build outputs to final `2.3.4` naming

## Highest-Priority Missing Features

If recovery time is tight, rebuild these first:

1. Gameplay mode config
2. Disabled-world support
3. Admin debug cooldown tools
4. Hidden client hotkey networking
5. Client HUD replacement

These unlock the largest amount of practical testing value.

## Recommended Working Strategy

For each phase:

1. Add the config or protocol shape first
2. Implement the server behavior
3. Verify with minimal local testing
4. Only then wire client display or polish
5. Update docs after the behavior is real

## Immediate Next Step

Start with Phase 1:

- extend `config.yml`
- add parsing helpers in the plugin/manager layer
- add world blocking hooks
- add mode detection helpers that gameplay code can query

This gives the rest of the rebuild a stable foundation.
