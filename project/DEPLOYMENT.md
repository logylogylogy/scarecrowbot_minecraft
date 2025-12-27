# ScarecrowBot - Deployment Guide

## Prerequisites

Before building and deploying the plugin, ensure you have:

1. **Java Development Kit (JDK) 21+**
   - Check: `java -version`
   - Download: https://adoptium.net/

2. **Apache Maven 3.6+**
   - Check: `mvn -version`
   - Download: https://maven.apache.org/download.cgi

3. **Paper Server 1.21.1+**
   - Download: https://papermc.io/downloads

## Building the Plugin

### Option 1: Using the build script (Linux/macOS)

```bash
chmod +x build.sh
./build.sh
```

### Option 2: Manual Maven build

```bash
mvn clean package
```

The compiled plugin will be located at:
```
target/ScarecrowBot-1.0-SNAPSHOT.jar
```

## Installation

1. **Stop your Paper server** (if running)

2. **Copy the JAR file** to your server's plugins folder:
   ```bash
   cp target/ScarecrowBot-1.0-SNAPSHOT.jar /path/to/server/plugins/
   ```

3. **Start your Paper server**

4. The plugin will create its configuration files:
   ```
   plugins/ScarecrowBot/
   ├── config.yml    # Configuration
   └── data.yml      # Scarecrow persistence (created after first spawn)
   ```

## First-Time Setup

### 1. Configure the Plugin

Edit `plugins/ScarecrowBot/config.yml` to customize:

- Bot name and chat format
- Entity type (VILLAGER or ZOMBIE)
- Health settings (maxHP, minHP)
- Chat response behavior
- Keyword triggers and replies

### 2. Create Your First Scarecrow

In-game, run:
```
/scarecrow create MyBotName
```

Or use default name from config:
```
/scarecrow create
```

The scarecrow will spawn at your current location.

## Usage Examples

### Basic Commands

```bash
# Create scarecrow with custom name
/scarecrow create TestBot

# Check status
/scarecrow status

# Move scarecrow to your location
/scarecrow move

# Teleport to scarecrow
/scarecrow tp

# Make scarecrow speak
/scarecrow say Hello everyone!

# Heal scarecrow
/scarecrow heal 50

# Damage scarecrow (for testing)
/scarecrow damage 25

# Remove scarecrow
/scarecrow remove

# Toggle chat responses
/scarecrow togglechat
```

### Testing Immortality

1. Create a scarecrow
2. Hit it with weapons to reduce HP
3. Watch HP decrease but never reach 0
4. Entity remains at minHP (default: 1.0)

### Testing Chat Responses

1. Ensure `bot.respondToChat: true` in config
2. Say a keyword like "hello" in chat
3. Scarecrow will respond based on configured rules
4. Random replies will occasionally trigger

### Testing Position Lock

1. Create a scarecrow with `lockToGround: true`
2. Push or hit the entity to move it
3. It will teleport back to spawn location instantly

## Configuration Examples

### Example 1: Silent Guardian

```yaml
scarecrow:
  entityType: "ZOMBIE"
  silent: true
  maxHP: 200.0
  minHP: 10.0
  lockToGround: true

bot:
  name: "Guardian"
  respondToChat: false  # Silent mode
```

### Example 2: Chatty Villager

```yaml
scarecrow:
  entityType: "VILLAGER"
  silent: false
  maxHP: 100.0
  minHP: 1.0

bot:
  name: "ChatBot"
  respondToChat: true
  randomReply:
    enabled: true
    chancePercent: 25  # Very chatty!
    messages:
      - "I'm always here!"
      - "Need something?"
      - "Hello!"
```

### Example 3: Korean Bot

```yaml
bot:
  name: "허수아비"
  chatFormat: "§e[{botName}]§f {message}"
  keywordReplies:
    rules:
      - keywords: ["안녕", "ㅎㅇ"]
        replies: ["안녕하세요!", "ㅎㅇㅎㅇ"]
      - keywords: ["뭐해", "뭐 해"]
        replies: ["여기 서있음", "그냥 있어요"]
      - keywords: ["죽어"]
        replies: ["난 안 죽음 ㅋㅋ", "불사신이에요"]
```

## Permissions Setup

### Full Admin Access
```yaml
groups:
  admin:
    permissions:
      - scarecrow.admin
```

### Limited Manager Access
```yaml
groups:
  moderator:
    permissions:
      - scarecrow.manage  # Can create, move, heal, etc.
      - scarecrow.say     # Can make bot speak
```

### Say-Only Access
```yaml
groups:
  trusted:
    permissions:
      - scarecrow.say     # Can only make bot speak
```

## Troubleshooting

### Scarecrow Doesn't Spawn

**Problem**: `/scarecrow create` does nothing or fails

**Solutions**:
- Check console for errors
- Verify you're on Paper 1.21.1+ (not Spigot/Bukkit)
- Ensure `entityType` in config is valid (VILLAGER or ZOMBIE)
- Check you have permission: `scarecrow.admin`

### Scarecrow Disappears After Restart

**Problem**: Scarecrow entity is gone after server restart

**Solutions**:
- Check if `data.yml` exists in `plugins/ScarecrowBot/`
- Verify the world name in `data.yml` matches loaded world
- Try removing and recreating the scarecrow
- Check console logs for "entity not found" warnings

### Chat Responses Not Working

**Problem**: Bot doesn't respond to chat

**Solutions**:
- Check `bot.respondToChat: true` in config
- Verify keywords are lowercase in config
- Check cooldowns aren't too high
- Test with `/scarecrow say test` to verify bot can speak

### Position Lock Not Working

**Problem**: Scarecrow moves when pushed

**Solutions**:
- Verify `scarecrow.lockToGround: true` in config
- Check console for task errors
- Position lock runs every 5 ticks (0.25s), slight delay is normal

### Build Errors

**Problem**: `mvn package` fails

**Solutions**:
- Ensure Java 21+ is installed
- Ensure Maven 3.6+ is installed
- Check internet connection (Maven downloads dependencies)
- Try `mvn clean install -U` to force update dependencies

## Advanced Configuration

### Custom Health Display Format

```yaml
scarecrow:
  showHpInName: true
  nameHpFormat: "§6{botName} §c❤ {hp}§7/§c{maxHp}"
```

### Multiple Chat Personalities

```yaml
bot:
  keywordReplies:
    rules:
      # Friendly responses
      - keywords: ["hi", "hello", "hey"]
        replies:
          - "Hello there!"
          - "Hi! How can I help?"
          - "Hey! Nice to see you!"

      # Help responses
      - keywords: ["help", "command", "how"]
        replies:
          - "Try /scarecrow status to check me!"
          - "I'm just a scarecrow, standing here!"

      # Sassy responses
      - keywords: ["die", "kill", "attack"]
        replies:
          - "Can't kill me!"
          - "I'm immortal lol"
          - "Nice try!"
```

### Disable Specific Features

```yaml
# Disable chat responses entirely
bot:
  respondToChat: false

# Disable position lock (allow pushing)
scarecrow:
  lockToGround: false

# Disable HP in name
scarecrow:
  showHpInName: false

# Disable damage effects
scarecrow:
  hurtSound: false
  damageParticles: false
```

## Performance Considerations

### Server Impact

- **Position Lock Task**: Runs every 5 ticks (0.25s) per scarecrow
  - Minimal impact: Single entity check and potential teleport
  - Disable with `lockToGround: false` if not needed

- **Chat Listener**: Monitors all player chat
  - Optimized with cooldowns to prevent spam
  - Early exit if responses disabled

- **Memory**: Scarecrow uses standard entity + minimal PDC data
  - Very low memory footprint

### Recommended Settings for High-Traffic Servers

```yaml
bot:
  respondToChat: true
  randomReply:
    chancePercent: 5              # Lower chance
    cooldownSecondsGlobal: 10     # Longer cooldowns
    cooldownSecondsPerPlayer: 20

  keywordReplies:
    cooldownSecondsGlobal: 5
    cooldownSecondsPerPlayer: 15
```

## Backup & Migration

### Backup Scarecrow Data

```bash
cp plugins/ScarecrowBot/data.yml backups/scarecrow-backup.yml
```

### Migrate to New Server

1. Copy the entire plugin folder:
   ```bash
   cp -r plugins/ScarecrowBot /path/to/new/server/plugins/
   ```

2. Ensure world names match (edit `data.yml` if needed)

3. Start new server, scarecrow will load automatically

## Uninstallation

1. Stop the server
2. Remove scarecrow in-game first (recommended):
   ```
   /scarecrow remove
   ```
3. Delete plugin JAR:
   ```bash
   rm plugins/ScarecrowBot-1.0-SNAPSHOT.jar
   ```
4. Optionally delete data folder:
   ```bash
   rm -r plugins/ScarecrowBot/
   ```

## Support & Development

### Logs

Check console/logs for detailed information:
- Plugin enable/disable messages
- Scarecrow creation/removal logs
- HP change notifications
- Error messages

### Debug Mode

Enable verbose logging in Paper's `config/paper-global.yml`:
```yaml
verbose: true
```

### Source Code

All source code is documented with comments explaining:
- Class responsibilities
- Method purposes
- Important implementation details

Read `PLAN.md` for architecture overview.

## Quick Reference Card

```
Commands:
  /scarecrow create [name]    - Spawn bot
  /scarecrow remove           - Remove bot
  /scarecrow move             - Move to you
  /scarecrow tp               - TP to bot
  /scarecrow status           - Show info
  /scarecrow heal <amount>    - Heal bot
  /scarecrow damage <amount>  - Damage bot
  /scarecrow say <message>    - Make bot speak
  /scarecrow togglechat       - Toggle responses

Permissions:
  scarecrow.admin   - Full access
  scarecrow.manage  - Create/modify
  scarecrow.say     - Make bot speak

Files:
  plugins/ScarecrowBot/config.yml  - Configuration
  plugins/ScarecrowBot/data.yml    - Bot data

Entity Types:
  VILLAGER (default) - Peaceful NPC look
  ZOMBIE             - Hostile mob look (AI disabled)
```

---

**Ready to deploy!** Follow the steps above and your immortal scarecrow bot will be protecting your server in no time.
