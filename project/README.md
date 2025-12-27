# ScarecrowBot - Minecraft Paper Plugin

An immortal scarecrow player bot for Minecraft Paper 1.21+ that can take damage, interact with players, and respond to chat messages.

## Features

- **Immortal Entity**: Scarecrow takes damage but never dies (HP stays at configured minimum)
- **Position Locking**: Entity is locked to spawn location and teleports back if moved
- **Chat Interaction**: Responds to keywords and randomly replies to player chat
- **Admin Commands**: Full control over scarecrow creation, movement, health, and behavior
- **Persistent Data**: Scarecrow survives server restarts
- **No Dependencies**: No NMS, ProtocolLib, or Citizens required

## Requirements

- Paper 1.21.1+
- Java 21+
- Maven 3.6+

## Building

```bash
mvn clean package
```

The compiled JAR will be in `target/ScarecrowBot-1.0-SNAPSHOT.jar`

## Installation

1. Build the plugin or download the JAR
2. Place JAR in your server's `plugins/` folder
3. Start/restart the server
4. Configure `plugins/ScarecrowBot/config.yml` as desired
5. Use `/scarecrow create` to spawn your bot

## How to Use (빠른 시작)

1. 서버의 `plugins/ScarecrowBot/config.yml`에서 기본 설정을 확인하세요. 특히 `bot.keywordReplies.rules`가 **`keywords`와 `replies` 키를 가진 맵 리스트** 형태로 되어 있는지 확인해야 합니다.
   ```yaml
   bot:
     respondToChat: true
     keywordReplies:
       enabled: true
       rules:
         - keywords:
             - "hi"
             - "hello"
           replies:
             - "Hello there!"
             - "Hi!"
   ```
2. 서버를 시작한 뒤 `/scarecrow create`로 허수아비를 소환합니다. 기본 이름(`Scarecrow`)을 사용하거나 `/scarecrow create 새이름`처럼 이름을 함께 입력해도 됩니다.
3. 플레이어가 채팅에 `hi`, `hello` 등 규칙에 포함된 단어를 입력하면 설정된 답변 중 하나를 무작위로 응답합니다.
4. 랜덤 응답이 필요하면 `bot.randomReply.enabled`를 `true`로 두고, 확률(`chancePercent`)과 쿨다운을 원하는 값으로 조정합니다.
5. 채팅 응답을 끄고 싶을 때는 `/scarecrow togglechat` 명령을 사용하거나 `respondToChat: false`로 설정 후 서버를 리로드/재시작하세요.

## Commands

| Command | Permission | Description |
|---------|-----------|-------------|
| `/scarecrow create [name]` | `scarecrow.admin` | Create a scarecrow at your location |
| `/scarecrow remove` | `scarecrow.admin` | Remove the scarecrow |
| `/scarecrow move` | `scarecrow.manage` | Move scarecrow to your location |
| `/scarecrow tp` | `scarecrow.manage` | Teleport to the scarecrow |
| `/scarecrow heal <amount>` | `scarecrow.manage` | Heal the scarecrow |
| `/scarecrow damage <amount>` | `scarecrow.manage` | Damage the scarecrow |
| `/scarecrow status` | `scarecrow.manage` | Show scarecrow status |
| `/scarecrow say <message>` | `scarecrow.say` | Make scarecrow speak in chat |
| `/scarecrow togglechat` | `scarecrow.admin` | Enable/disable chat responses |

## Permissions

- `scarecrow.admin` - Full administrative access (OP by default)
- `scarecrow.manage` - Manage scarecrow (create, move, heal, etc.)
- `scarecrow.say` - Make scarecrow speak

## Configuration

Edit `plugins/ScarecrowBot/config.yml`:

### Bot Settings

```yaml
bot:
  name: "Scarecrow"              # Display name
  chatFormat: "<{botName}> {message}"  # Chat format
  respondToChat: true            # Enable chat responses
```

### Random Replies

```yaml
  randomReply:
    enabled: true
    chancePercent: 10            # 10% chance to reply
    cooldownSecondsGlobal: 3     # Global cooldown
    cooldownSecondsPerPlayer: 8  # Per-player cooldown
    messages:
      - "..."
      - "ㅎㅎ"
```

### Keyword Replies

```yaml
  keywordReplies:
    enabled: true
    rules:
      - keywords: ["hi", "hello"]
        replies: ["Hi!", "Hello there!"]
```

### Scarecrow Entity Settings

```yaml
scarecrow:
  entityType: "VILLAGER"         # VILLAGER or ZOMBIE
  visibleName: true              # Show custom name
  lockToGround: true             # Prevent movement
  silent: true                   # No entity sounds
  maxHP: 100.0                   # Maximum health
  minHP: 1.0                     # Minimum health (prevents death)
  showHpInName: true             # Show HP in name
  nameHpFormat: "{botName} §c[HP {hp}/{maxHp}]"
```

## How It Works

### Damage System

1. Scarecrow stores HP in PersistentDataContainer (PDC)
2. When damaged, HP decreases but never goes below `minHP`
3. Entity health bar reflects current HP percentage
4. Damage feedback includes hurt sounds and particles

### Position Lock

- Repeating task (every 5 ticks) checks entity position
- If moved >0.1 blocks from spawn, teleports back instantly
- Velocity is zeroed to prevent knockback accumulation

### Chat System

- Listens to `AsyncChatEvent` (runs async)
- Checks keyword rules first, then random reply chance
- Respects global and per-player cooldowns
- Broadcasts reply via sync task (Bukkit-safe)

### Persistence

- Saves to `data.yml` on disable and after changes
- Stores: UUID, world, location, HP, name
- Loads entity by UUID on startup
- If entity not found, logs warning (manual respawn needed)

## Technical Details

- **Entity Types**: Villager (default) or Zombie with AI disabled
- **Data Storage**: PersistentDataContainer + YAML file
- **Async Safety**: Chat listener schedules sync tasks for Bukkit operations
- **Lombok**: Used for boilerplate reduction (@Getter, @NoArgsConstructor, etc.)
- **Gson**: Shaded dependency for potential JSON operations

## Development

### Project Structure

```
src/main/
├── java/com/example/scarecrowbot/
│   ├── ScarecrowBotPlugin.java           # Main plugin class
│   ├── manager/
│   │   ├── ScarecrowManager.java         # Entity lifecycle & persistence
│   │   └── CooldownManager.java          # Chat cooldown tracking
│   ├── listener/
│   │   ├── DamageListener.java           # Damage & death events
│   │   └── ChatListener.java             # Chat response logic
│   ├── command/
│   │   ├── ScarecrowCommand.java         # Command executor
│   │   └── ScarecrowTabCompleter.java    # Tab completion
│   └── util/
│       └── PDCKeys.java                   # PDC key constants
└── resources/
    ├── plugin.yml                         # Plugin metadata
    └── config.yml                         # Default config
```

### Key Classes

- **ScarecrowBotPlugin**: Plugin lifecycle, static instance access
- **ScarecrowManager**: Core logic for entity creation, HP, persistence, position lock
- **CooldownManager**: Tracks global and per-player cooldowns with expiration
- **DamageListener**: Prevents death, manages HP on damage
- **ChatListener**: Keyword and random replies with async safety
- **ScarecrowCommand**: All subcommand implementations

## License

This is a demonstration project. Use freely.

## Support

For issues or questions, check the source code and comments - everything is documented.
