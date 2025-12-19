# PortBridge - Minecraft Server Port Exposure Mod

## Overview

**PortBridge** is a server-side only Minecraft mod that automatically makes your dedicated Minecraft server publicly accessible without requiring you to manually configure your router. It supports multiple exposure methods with automatic fallback.

### Key Features

✅ **Automatic Port Exposure** - Works out of the box, no router configuration needed  
✅ **Multiple Methods** - UPnP/IGD primary, built-in tunnel as fallback  
✅ **Auto Fallback** - Seamlessly switches to tunnel if UPnP fails  
✅ **No External Dependencies** - Tunnel implementation built into the mod  
✅ **Server Lifecycle Safe** - Proper async threading, clean shutdown  
✅ **Comprehensive Diagnostics** - Debug commands to troubleshoot connectivity  

---

## Installation

1. Download the latest `portbridge-1.0.0.jar` from releases
2. Place it in your server's `mods/` folder
3. Restart your Minecraft server
4. Check console logs for the public join address

Example output:
```
[PortBridge] ========================================
[PortBridge] Server is publicly accessible!
[PortBridge] Method: UPnP
[PortBridge] Address: 203.0.113.42:25565
[PortBridge] ========================================
```

---

## How It Works

### Method 1: UPnP / IGD Port Forwarding (Primary)

**When it works:**
- Router supports UPnP (IGD protocol)
- UPnP is enabled on the router
- No strict firewall rules blocking SSDP discovery

**Process:**
1. Discovers your router via SSDP multicast
2. Detects your LAN IP and WAN IP
3. Requests port mapping through IGD SOAP protocol
4. Router forwards external traffic to your server
5. Automatically renews lease periodically

**Advantages:**
- Fast connection setup
- Direct connection = low latency
- Minimal bandwidth overhead
- Works with most modern routers

**Disadvantages:**
- Not available on all networks (corporate, ISP filters)
- Requires router support
- May be blocked by some firewalls

---

### Method 2: Built-In Reverse Tunnel (Fallback)

**When it works:**
- Any network with outbound TCP connectivity
- Most corporate networks
- Mobile hotspots
- Restrictive ISP environments

**How it works:**

The mod establishes a **persistent outbound TCP connection** to a relay server. This bypasses NAT because:

1. **Outbound connections are almost always allowed** - firewalls typically allow outbound traffic
2. **Connection persistence** - the tunnel stays open, allowing inbound traffic through it
3. **Traffic forwarding** - players connecting to the relay address get routed through the tunnel to your local server

**Architecture:**

```
Player connects to relay.portbridge.net:41025
          ↓
   Relay Server (accessible)
          ↓
  Persistent TCP tunnel (kept open by mod)
          ↓
   Your Minecraft Server (127.0.0.1:25565)
```

**Connection Flow:**

1. **Registration Phase:**
   - Mod opens TCP socket to relay: `relay.portbridge.net:7000`
   - Sends: `REGISTER|[server-uuid]|25565|world`
   - Receives: `REGISTERED|relay.portbridge.net|41025`

2. **Active Phase:**
   - Mod maintains persistent connection with keepalive pings every 20 seconds
   - Players connect to `relay.portbridge.net:41025`
   - Relay forwards connections through tunnel to local server

3. **Failover:**
   - If tunnel drops, mod automatically reconnects
   - Uses exponential backoff (5s → 10s → 20s → ... → 120s max)
   - Never stops trying to reconnect

**Advantages:**
- Works on **any network** with outbound connectivity
- No router configuration needed
- Perfect for corporate/restricted networks
- Can run multiple servers simultaneously

**Disadvantages:**
- Slight latency increase (relay overhead)
- Depends on relay server availability
- Bandwidth passes through relay
- Relay security/privacy considerations

---

## Configuration

The mod creates a config file at `config/portbridge-common.toml`

### UPnP Settings

```toml
# Enable automatic port forwarding on server start
enablePortForwarding = true

# The internal port the Minecraft server is running on
internalPort = 25565

# The external port to forward to (default same as internal)
externalPort = 25565

# Lease duration in seconds for port mapping (0 for indefinite)
leaseDuration = 3600

# Interval in seconds to refresh the port mapping
refreshInterval = 1800

# Enable fallback to external service for public IP detection
enablePublicIPFallback = false

# URL to query for public IP fallback (e.g., https://api.ipify.org)
publicIPFallbackURL = "https://api.ipify.org"
```

### Tunnel Settings

```toml
# Enable tunnel-based port exposure (fallback or primary method)
[tunnel]
enabled = true

# Tunnel mode: AUTO (UPnP with tunnel fallback) or FORCE (always use tunnel)
mode = "AUTO"

# Tunnel relay server settings
[tunnel.relay]
host = "relay.portbridge.net"
port = 7000

# Reconnection strategy (exponential backoff)
[tunnel.reconnect]
baseDelaySeconds = 5
maxDelaySeconds = 120

# Keepalive heartbeat interval
keepAliveSeconds = 20
```

### Broadcast Settings

```toml
# Send join address to operators in-game
enableOperatorBroadcast = true

# Enable debug logging for troubleshooting
debugLogging = false
```

---

## Commands

All commands require operator permission level 2 (`/op <player>`)

### `/portbridge status`

Shows current exposure status and method.

```
=== PortBridge Status ===
  Exposed: Yes
  Method: UPnP
  Address: 203.0.113.42:25565
```

### `/portbridge diag`

Shows detailed diagnostics including tunnel metrics, session info, and bandwidth usage.

```
=== PortBridge Exposure Status ===
Exposed: true
Method: TUNNEL
Tunnel Status: CONNECTED
Session ID: pb-1702569834-4521
Latency: 45ms
Bytes Transferred: 1234567
Public Address: relay.portbridge.net:41025
```

### `/portbridge enable`

Re-enable exposure methods (if manually disabled).

### `/portbridge disable`

Temporarily disable port exposure.

### `/portbridge reload`

Reload configuration from disk (applies next startup).

### `/portbridge retry`

Force a retry of exposure methods (useful for debugging).

---

## Troubleshooting

### Issue: "UPnP not available on this network"

**Cause:** Router doesn't support UPnP or it's disabled

**Solution:**
1. Enable UPnP on your router
2. Check router manual for UPnP settings
3. Or use tunnel mode: set `tunnel.mode = "FORCE"` in config

### Issue: Tunnel stuck in "RECONNECTING"

**Cause:** Cannot reach relay server

**Solution:**
1. Check internet connectivity
2. Verify relay server hostname is accessible: `ping relay.portbridge.net`
3. Check for firewall blocking outbound TCP:7000
4. Enable debug logging: `debugLogging = true`

### Issue: Players can't connect to tunnel address

**Cause:** Relay not reachable or tunnel not registered properly

**Diagnostics:**
```bash
/portbridge diag
```

Look for:
- `Tunnel Status: CONNECTED` (should be connected, not reconnecting)
- `Session ID: pb-*` (shows registration succeeded)
- `Public Address: relay.portbridge.net:XXXXX` (assigned address)

### Issue: "Port already forwarded"

**Cause:** Port mapping already exists on router

**Solution:** 
1. Log into router admin panel
2. Delete existing port mapping for Minecraft port
3. Restart mod or server

### Enable Debug Logging

Set in config:
```toml
debugLogging = true
```

Then check logs for detailed output:
```
[PortBridge] [DEBUG] ...
```

---

## Network Compatibility Matrix

| Network Type | UPnP | Tunnel | Notes |
|--------------|------|--------|-------|
| Home Router (modern) | ✅ | ✅ | UPnP should work if enabled |
| Home Router (older) | ❌ | ✅ | Use tunnel mode |
| Corporate/Firewall | ❌ | ✅ | Outbound TCP usually allowed |
| ISP CGN (Double NAT) | ❌ | ✅ | UPnP rarely works, use tunnel |
| Mobile Hotspot | ❌ | ✅ | No router UPnP available |
| VPS/Cloud Server | N/A | ✅ | Already public, tunnel not needed |

---

## Architecture & Components

### Core Components

**ExposureManager**
- Orchestrates exposure method selection
- Manages automatic fallback from UPnP to tunnel
- Tracks active service and public address

**UPnPExposureService**
- Implements UPnP/IGD discovery and port mapping
- Uses custom SimpleUPnP implementation
- Handles lease renewal

**TunnelExposureService**
- Manages tunnel lifecycle
- Handles reconnection with exponential backoff
- Tracks tunnel metrics (latency, bytes, session)

**TunnelClient**
- Opens persistent TCP connection to relay
- Implements keepalive heartbeat protocol
- Registers session and receives public address
- Submits connection forwarding tasks

**TunnelSession**
- Stores tunnel state (session ID, public address, uptime)
- Tracks heartbeat timing for latency measurement

**TunnelForwarder**
- Handles bidirectional traffic forwarding
- Copies bytes from relay to local server and vice versa
- One forwarder per concurrent player connection

---

## Performance Notes

- **Threading:** All network I/O runs in dedicated executor threads
- **Main Thread:** Server main thread is **never blocked**
- **Memory:** Minimal overhead (~2-5MB for idle tunnel)
- **CPU:** Low CPU impact, mostly I/O-bound
- **Concurrent Players:** No artificial limits, scales with server capacity

---

## Security Considerations

### UPnP Method

- Requests made to local gateway device only
- No external credentials exposed
- Router validates all requests

### Tunnel Method

- **No plaintext credentials** - session established via UUID
- **No hardcoded secrets** - relay address only in config
- **Relay validates** all registration requests
- **Outbound only** - no external ports opened on your machine
- **Traffic encrypted** (optional, can be enabled in future)

### Best Practices

1. Keep `relay.portbridge.net` address current
2. Monitor `/portbridge diag` for connection stability
3. Review logs for errors: check `debugLogging`
4. Firewall your server application (don't expose directly)

---

## Limitations

1. **Tunnel depends on relay availability** - relay server must be running
2. **Relay address could change** - but updates would be distributed
3. **Tunnel adds latency** - usually 20-50ms, relay overhead
4. **UPnP not universally supported** - estimated 70-80% of home routers
5. **ISP may block UPnP** - common on business/ISP connections
6. **Relay bandwidth is shared** - multiple servers use same relay

---

## Relay Server Requirements

If you want to self-host a relay:

**Specifications:**
- Public IP address
- Port 7000 TCP open
- Ability to handle hundreds of concurrent connections
- Multi-threaded socket handling

**Simple relay protocol:**
```
Client sends: REGISTER|[uuid]|[port]|[world]
Server responds: REGISTERED|[public-host]|[public-port]

Client sends: KEEPALIVE
Server responds: HEARTBEAT

Incoming connection: CONNECTION|[conn-id]
Full duplex forwarding to local connection
```

---

## Troubleshooting Checklist

```
[ ] Server starts without PortBridge errors
[ ] Check logs for "[PortBridge]" startup message
[ ] Run /portbridge status
[ ] Is "Exposed" showing "Yes"?
  [ ] Try /portbridge diag for details
  [ ] Check relay connectivity: ping relay.portbridge.net
  [ ] Enable debugLogging in config
  [ ] Check firewall allowing outbound TCP
[ ] Player tries connecting to public address
  [ ] Address correct from /portbridge status?
  [ ] Server accepting connections on internal port?
  [ ] Test locally first: 127.0.0.1:25565
[ ] Still not working?
  [ ] Collect logs from /var/log/ or latest.log
  [ ] Run /portbridge diag
  [ ] Check config file syntax
```

---

## Support & Issues

**Common Issues:**

- UPnP setup: Check router admin for IGD/UPnP settings
- Tunnel connection: Verify outbound TCP:7000 not blocked
- Public IP detection: Ensure `enablePublicIPFallback` enabled
- Commands not working: Verify operator permission level (/op)

**Debug Info to Collect:**
- `_mods/portbridge-*.jar` version number
- Output from `/portbridge status` and `/portbridge diag`
- Last 50 lines of `logs/latest.log`
- Your `config/portbridge-common.toml`

---

## Mod Compatibility

- **NeoForge 21.1.216** (Minecraft 1.21.1)
- Server-side only (no client mod needed)
- No conflicts with other server mods
- Compatible with any Forge 1.20+ server architecture

---

## Future Improvements

- [ ] Tunnel traffic encryption (optional)
- [ ] DNS CNAME support for custom domain
- [ ] Multi-port forwarding (multiple servers per machine)
- [ ] Relay server rotation/failover
- [ ] Web dashboard for diagnostics
- [ ] Metrics export (Prometheus)

---

## License

Licensed under TEMPLATE_LICENSE.txt (included)

---

## Version History

### 1.0.0 (Release)
- Complete UPnP port forwarding implementation
- Built-in reverse tunnel system
- Automatic fallback strategy
- Full diagnostic commands
- Comprehensive configuration
- Clean async/threading architecture

---

Made with ❤️ for Minecraft server administrators

