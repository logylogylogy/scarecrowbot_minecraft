# Scarecrow Player Bot - Implementation Plan

## Project Overview
A Paper 1.21.10 plugin that creates an immortal, stationary NPC-like entity (scarecrow) that can take damage, chat, and respond to players without using NMS/ProtocolLib/Citizens.

## Architecture

### Core Components

#### 1. Main Plugin Class (`ScarecrowBotPlugin.java`)
- Extends `JavaPlugin`
- Static instance with `getInstance()` method
- Manages plugin lifecycle (onEnable, onDisable)
- Initializes all managers, listeners, commands
- Loads config and data files

#### 2. Manager Classes

**ScarecrowManager.java**
- Singleton pattern accessing plugin via `getInstance()`
- Create scarecrow entity (Villager or Zombie based on config)
- Load/save scarecrow data from `data.yml`
- Position lock enforcement (repeating task teleports back if moved)
- HP management via PersistentDataContainer
- Name display with HP indicator
- Persist entity UUID, location, HP, name

**CooldownManager.java**
- Track global and per-player cooldowns
- Methods: `isOnCooldown(String key)`, `isOnCooldown(UUID player, String key)`, `setCooldown(...)`
- Store cooldowns with expiration timestamps

#### 3. Listener Classes

**DamageListener.java**
- `EntityDamageEvent`: Check if entity is scarecrow, apply damage, update HP in PDC, prevent death
- `EntityDeathEvent`: Cancel drops/exp for scarecrow, prevent actual death

**ChatListener.java**
- `AsyncChatEvent`: Listen to player chat
- Check keyword replies and random replies based on config
- Respect cooldowns (global + per-player)
- Schedule sync tasks for chat broadcasts and entity operations

#### 4. Command Classes

**ScarecrowCommand.java**
- Main command executor for `/scarecrow`
- Subcommands: create, remove, move, tp, heal, damage, status, say, togglechat
- Permission checks
- Null-safe argument parsing
- Clear feedback messages

**ScarecrowTabCompleter.java**
- Tab completion for subcommands
- Context-aware suggestions (e.g., entity types for create)

#### 5. Utility Classes

**PDCKeys.java**
- Static final NamespacedKey constants
- Keys: `SCARECROW_MARKER`, `SCARECROW_HP`, `SCARECROW_MAX_HP`, `SCARECROW_NAME`

**ConfigManager.java** (optional wrapper)
- Helper methods to safely read config values with defaults

## Data Flow

### Scarecrow Creation Flow
1. `/scarecrow create [name]` command received
2. Validate no existing scarecrow
3. Spawn entity at player location (type from config)
4. Set AI to false, persistent to true, removeWhenFarAway to false
5. Apply PDC markers (SCARECROW_MARKER, HP, MAX_HP, NAME)
6. Set custom name with HP display
7. Save to `data.yml` (UUID, world, x, y, z, yaw, pitch, hp, name)
8. Start position lock task if not running
9. Send success message

### Damage Flow
1. `EntityDamageEvent` fired
2. Check if entity has SCARECROW_MARKER in PDC
3. If yes: read current HP from PDC
4. Calculate new HP = current HP - final damage
5. If new HP <= 0: set new HP = minHP (prevent death)
6. Update HP in PDC
7. Update entity health attribute
8. Update name display with new HP
9. Play hurt sound and particles
10. If HP reached minHP: optionally broadcast "broken" message

### Death Prevention Flow
1. Additional check in damage handler: if new HP <= minHP, cancel event
2. `EntityDeathEvent` listener: if entity is scarecrow, cancel event, clear drops
3. Ensure entity remains at minHP and alive

### Position Lock Flow
1. Repeating task runs every 5 ticks (0.25s)
2. Check if scarecrow entity exists and is loaded
3. If entity location differs from stored spawn location (distance > 0.1)
4. Teleport entity back to spawn location
5. Optionally set velocity to zero

### Chat Response Flow
1. `AsyncChatEvent` fired (async thread)
2. Extract message content
3. Check if chat response is enabled in config
4. Check keyword replies first:
   - Check global cooldown for "keyword"
   - Check per-player cooldown for "keyword"
   - Search message for keywords
   - If match found and not on cooldown: select random reply, set cooldowns
5. If no keyword match, check random reply:
   - Check if enabled and roll chance (random % < chancePercent)
   - Check cooldowns
   - Select random message
6. Schedule sync task to broadcast reply formatted as bot message

### Save/Load Flow

**Save (onDisable or command)**
- Get scarecrow entity UUID, location, HP
- Write to `data.yml`:
  ```yaml
  scarecrow:
    exists: true
    uuid: "..."
    world: "world"
    x: 0.0
    y: 64.0
    z: 0.0
    yaw: 0.0
    pitch: 0.0
    hp: 100.0
    name: "Scarecrow"
  ```

**Load (onEnable)**
- Read `data.yml`
- If scarecrow exists = true: find entity by UUID in world
- If found: load HP from file, sync with PDC, update display
- If not found: log warning (entity was unloaded/killed)
- Start position lock task

## File Structure

```
ScarecrowBot/
├── pom.xml
├── PLAN.md
├── src/main/
│   ├── java/com/example/scarecrowbot/
│   │   ├── ScarecrowBotPlugin.java
│   │   ├── manager/
│   │   │   ├── ScarecrowManager.java
│   │   │   └── CooldownManager.java
│   │   ├── listener/
│   │   │   ├── DamageListener.java
│   │   │   └── ChatListener.java
│   │   ├── command/
│   │   │   ├── ScarecrowCommand.java
│   │   │   └── ScarecrowTabCompleter.java
│   │   └── util/
│   │       └── PDCKeys.java
│   └── resources/
│       ├── plugin.yml
│       └── config.yml
└── target/ (generated by Maven)
```

## Config Keys (config.yml)

```yaml
bot:
  name: "Scarecrow"
  chatFormat: "<{botName}> {message}"
  respondToChat: true
  randomReply:
    enabled: true
    chancePercent: 10
    cooldownSecondsGlobal: 3
    cooldownSecondsPerPlayer: 8
    messages: [...]
  keywordReplies:
    enabled: true
    cooldownSecondsGlobal: 2
    cooldownSecondsPerPlayer: 6
    rules:
      - keywords: [...]
        replies: [...]

scarecrow:
  entityType: "VILLAGER"
  visibleName: true
  lockToGround: true
  silent: true
  invulnerable: false
  maxHP: 100.0
  minHP: 1.0
  showHpInName: true
  nameHpFormat: "{botName} [HP {hp}/{maxHp}]"
```

## Commands & Permissions

| Command | Permission | Description |
|---------|-----------|-------------|
| `/scarecrow create [name]` | `scarecrow.admin` | Create scarecrow |
| `/scarecrow remove` | `scarecrow.admin` | Remove scarecrow |
| `/scarecrow move` | `scarecrow.manage` | Move to sender location |
| `/scarecrow tp` | `scarecrow.manage` | TP sender to scarecrow |
| `/scarecrow heal [amount]` | `scarecrow.manage` | Heal scarecrow |
| `/scarecrow damage [amount]` | `scarecrow.manage` | Damage scarecrow |
| `/scarecrow status` | `scarecrow.manage` | Show status |
| `/scarecrow say <message>` | `scarecrow.say` | Make scarecrow speak |
| `/scarecrow togglechat` | `scarecrow.admin` | Toggle chat responses |

## Maven Dependencies

- `io.papermc.paper:paper-api:1.21.10-R0.1-SNAPSHOT` (provided)
- `org.projectlombok:lombok:1.18.42` (provided)
- `com.google.code.gson:gson:2.10.1` (shaded)

## Key Implementation Details

### PDC Keys
- Namespace: plugin name
- SCARECROW_MARKER: PersistentDataType.BOOLEAN (true)
- SCARECROW_HP: PersistentDataType.DOUBLE
- SCARECROW_MAX_HP: PersistentDataType.DOUBLE
- SCARECROW_NAME: PersistentDataType.STRING

### Entity Configuration
- Disable AI: `entity.setAI(false)`
- Make persistent: `entity.setPersistent(true)`, `entity.setRemoveWhenFarAway(false)`
- Silent: `entity.setSilent(true)`
- Custom name: `entity.setCustomName(Component.text(...))`, `entity.setCustomNameVisible(true)`
- Max health: Use AttributeInstance for GENERIC_MAX_HEALTH

### Async Safety
- AsyncChatEvent runs on async thread
- All Bukkit entity/world modifications must be scheduled sync
- Use `Bukkit.getScheduler().runTask(plugin, () -> {...})`

### Null Safety
- Check all entity lookups for null
- Check all config values with defaults
- Check all command arguments with validation
- Log warnings and provide user feedback

## Testing Checklist

- [ ] Create scarecrow with default name
- [ ] Create scarecrow with custom name
- [ ] Damage scarecrow and verify HP decreases
- [ ] Damage to 0 and verify it stays at minHP
- [ ] Verify scarecrow doesn't drop items on death event
- [ ] Verify scarecrow doesn't despawn
- [ ] Push scarecrow and verify it teleports back
- [ ] Reload plugin and verify scarecrow persists
- [ ] Test all commands
- [ ] Test chat keyword responses
- [ ] Test random chat responses
- [ ] Verify cooldowns work correctly
- [ ] Test permissions
- [ ] Verify async chat safety

## Build & Deployment

```bash
mvn clean package
# Output: target/ScarecrowBot-1.0-SNAPSHOT.jar
# Copy to server plugins/ folder
# Start server, test commands
```
