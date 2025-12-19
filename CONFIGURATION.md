# PortBridge Configuration Guide

This guide explains all configuration options for PortBridge.

## Configuration File Location

`config/portbridge-common.toml`

The file is auto-generated on first server start with default values.

---

## Complete Configuration Reference

### UPnP Port Forwarding

#### `enablePortForwarding`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enables automatic UPnP port forwarding on server start
- **Values:** `true` or `false`

#### `internalPort`
- **Type:** Integer (1-65535)
- **Default:** `25565`
- **Description:** The port your Minecraft server listens on (localhost:internalPort)
- **Note:** Must match the server.properties `server-port` setting

#### `externalPort`
- **Type:** Integer (1-65535)
- **Default:** `25565`
- **Description:** The port exposed to the internet
- **Note:** Usually same as internalPort. Different values allow port remapping.

#### `leaseDuration`
- **Type:** Integer (seconds)
- **Default:** `3600` (1 hour)
- **Range:** 0 to 2,147,483,647
- **Description:** How long the router keeps the port mapping active
- **Values:**
  - `0` = Indefinite (some routers may not support)
  - `3600` = 1 hour (recommended, auto-renewed)
  - `86400` = 24 hours

#### `refreshInterval`
- **Type:** Integer (seconds)
- **Default:** `1800` (30 minutes)
- **Range:** 60 to 2,147,483,647
- **Description:** How often to refresh/renew port mapping with router
- **Note:** Auto-set to leaseDuration/2 if leaseDuration > 0

#### `retryCount`
- **Type:** Integer
- **Default:** `3`
- **Range:** 0 to 10
- **Description:** Number of retries if UPnP discovery/mapping fails
- **Note:** Mostly obsolete with AUTO tunnel fallback

#### `retryDelay`
- **Type:** Integer (seconds)
- **Default:** `5`
- **Range:** 1 to 60
- **Description:** Delay between retry attempts
- **Note:** Not used if tunnel fallback is enabled

### Public IP Detection

#### `enablePublicIPFallback`
- **Type:** Boolean
- **Default:** `false`
- **Description:** Enable fallback IP detection via external HTTP service
- **When needed:** If UPnP can't determine external IP
- **Privacy note:** Queries external service, IP may be logged

#### `publicIPFallbackURL`
- **Type:** String (URL)
- **Default:** `"https://api.ipify.org"`
- **Description:** HTTP API endpoint that returns your public IP
- **Other options:**
  - `https://api.ipify.org` (responds with plain IP)
  - `https://checkip.amazonaws.com` (AWS service)
  - `https://icanhazip.com` (simple IP service)

### Tunnel Configuration

#### `tunnel.enabled`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Enable tunnel exposure method
- **Note:** Required for AUTO and FORCE modes

#### `tunnel.mode`
- **Type:** String
- **Default:** `"AUTO"`
- **Values:**
  - `"AUTO"` = Try UPnP first, fall back to tunnel if it fails
  - `"FORCE"` = Always use tunnel, skip UPnP entirely

**AUTO Mode Flow:**
```
Start → Try UPnP (10 second timeout)
  ├─ Success → Use UPnP, skip tunnel
  └─ Failure → Switch to tunnel
```

**FORCE Mode Flow:**
```
Start → Immediately connect to tunnel
```

#### `tunnel.relay.host`
- **Type:** String (hostname or IP)
- **Default:** `"relay.portbridge.net"`
- **Description:** Relay server hostname
- **Note:** Must be publicly resolvable

#### `tunnel.relay.port`
- **Type:** Integer (1-65535)
- **Default:** `7000`
- **Description:** Relay server port for tunnel connections
- **Note:** Must match relay server configuration

### Tunnel Reconnection Strategy

#### `tunnel.reconnect.baseDelaySeconds`
- **Type:** Integer
- **Default:** `5`
- **Range:** 1 to 60
- **Description:** Initial delay before first reconnection attempt
- **Example:** If tunnel drops, wait 5 seconds before retry

#### `tunnel.reconnect.maxDelaySeconds`
- **Type:** Integer
- **Default:** `120`
- **Range:** 10 to 3600
- **Description:** Maximum delay between reconnection attempts
- **Note:** Uses exponential backoff (baseDelay × 2^attemptCount, capped at maxDelay)

**Reconnection Backoff Example:**
```
Attempt 1: 5 seconds
Attempt 2: 10 seconds
Attempt 3: 20 seconds
Attempt 4: 40 seconds
Attempt 5: 80 seconds
Attempt 6+: 120 seconds (max)
```

#### `tunnel.keepAliveSeconds`
- **Type:** Integer
- **Default:** `20`
- **Range:** 5 to 120
- **Description:** Keepalive heartbeat interval
- **Purpose:** Prevents relay timeout, keeps tunnel connection alive
- **Note:** Mod handles this automatically

### Operator Broadcast

#### `enableOperatorBroadcast`
- **Type:** Boolean
- **Default:** `true`
- **Description:** Send public address to online operators in-game
- **Example message:** `PortBridge: UPnP → 203.0.113.42:25565`

### Debugging

#### `debugLogging`
- **Type:** Boolean
- **Default:** `false`
- **Description:** Enable verbose debug logging
- **When to use:** Troubleshooting connection issues
- **Output:** Appears in `logs/latest.log`

**Debug Log Examples:**
```
[PortBridge] [DEBUG] Public IP from UPnP: 203.0.113.42
[PortBridge] [DEBUG] Tunnel client created
[PortBridge] [DEBUG] Refreshed UPnP port mapping
```

---

## Common Configuration Scenarios

### Scenario 1: Home Network (Recommended Default)

```toml
enablePortForwarding = true
tunnel.enabled = true
tunnel.mode = "AUTO"
tunnel.relay.host = "relay.portbridge.net"
```

**Why:** UPnP usually works, tunnel available as instant fallback.

### Scenario 2: Corporate/Restricted Network

```toml
enablePortForwarding = false
tunnel.enabled = true
tunnel.mode = "FORCE"
```

**Why:** UPnP likely blocked by firewall, tunnel guaranteed to work.

### Scenario 3: VPS / Cloud Hosting

```toml
enablePortForwarding = false
tunnel.enabled = false
```

**Why:** Server already has public IP, no exposure needed. Comment out PortBridge or disable entirely.

### Scenario 4: Advanced - Custom Relay

```toml
tunnel.enabled = true
tunnel.relay.host = "my-relay.example.com"
tunnel.relay.port = 9000
tunnel.reconnect.maxDelaySeconds = 60
```

**Why:** Self-hosted relay server with custom settings.

### Scenario 5: Debugging Mode

```toml
debugLogging = true
tunnel.mode = "FORCE"
tunnel.reconnect.baseDelaySeconds = 2
tunnel.reconnect.maxDelaySeconds = 10
```

**Why:** Verbose logging + faster reconnection for troubleshooting.

---

## Performance Tuning

### For High-Performance Servers

```toml
# Reduce refresh frequency (less router chatter)
refreshInterval = 3600

# Increase keepalive if tunnel very stable
tunnel.keepAliveSeconds = 30

# Longer max reconnect delay (avoid spam)
tunnel.reconnect.maxDelaySeconds = 300
```

### For Unstable Networks

```toml
# More frequent refreshes
refreshInterval = 900

# More frequent keepalives
tunnel.keepAliveSeconds = 10

# Faster reconnection attempts
tunnel.reconnect.baseDelaySeconds = 2
tunnel.reconnect.maxDelaySeconds = 60
```

---

## Configuration Validation

The mod validates configuration on startup. Invalid values are logged:

```
[PortBridge] Warning: internalPort must be 1-65535, got 99999
[PortBridge] Using default value: 25565
```

All invalid configs are replaced with defaults.

---

## Reloading Configuration

To reload configuration after editing the file:

1. Edit `config/portbridge-common.toml`
2. Run `/portbridge reload` in-game OR restart server
3. Check logs for success message

---

## Troubleshooting Configuration

### Issue: Changes not taking effect

**Solution:** 
- Edit config file (not running)
- Restart server completely
- Or use `/portbridge reload` command

### Issue: Invalid configuration error

**Solution:**
- Check TOML syntax (quotes, equals signs)
- Ensure ports are 1-65535
- Verify URL format for fallback IP service

### Issue: Can't find config file

**Solution:**
```bash
# Generate default config
# Start server once, then stop
# File creates at:
config/portbridge-common.toml
```

### Issue: Port 25565 already in use

**Solution:**
- Change `server.properties` port
- Update `internalPort` to match
- Both must be identical

---

## Config File Format (TOML)

PortBridge uses TOML format. Basic syntax:

```toml
# Comments start with #

# Boolean values
enablePortForwarding = true
enablePortForwarding = false

# Numbers (integers)
internalPort = 25565
leaseDuration = 3600

# Strings (quoted)
tunnel.relay.host = "relay.portbridge.net"
publicIPFallbackURL = "https://api.ipify.org"
```

---

## Default Configuration (On First Run)

```toml
# UPnP Configuration
enablePortForwarding = true
internalPort = 25565
externalPort = 25565
leaseDuration = 3600
refreshInterval = 1800
retryCount = 3
retryDelay = 5

# Public IP Detection
enablePublicIPFallback = false
publicIPFallbackURL = "https://api.ipify.org"

# Operator Broadcast
enableOperatorBroadcast = true

# Tunnel Configuration
[tunnel]
enabled = true
mode = "AUTO"

[tunnel.relay]
host = "relay.portbridge.net"
port = 7000

[tunnel.reconnect]
baseDelaySeconds = 5
maxDelaySeconds = 120

keepAliveSeconds = 20

# Debugging
debugLogging = false
```

---

## Advanced: Self-Hosted Relay Setup

If you want to run your own relay server:

**Minimum Requirements:**
- Public IP / hostname
- Java 11+ runtime
- Port 7000 TCP open
- 100MB+ disk for logs

**Example Relay Protocol:**
```java
// Listen on 0.0.0.0:7000
// For each client connection:
//   Read: REGISTER|uuid|port|world
//   Send: REGISTERED|public-host|assigned-port
//   Forward connections to local-ip:port
//   Keep connection alive with HEARTBEAT/ACK
```

Relay implementation out of scope for this mod, but protocol is simple.

---

## Support

For configuration issues:
- Run `/portbridge diag` to test current setup
- Enable `debugLogging = true` and check logs
- Verify config file TOML syntax
- Check firewall/network restrictions

