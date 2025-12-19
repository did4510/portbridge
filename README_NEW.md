
# PortBridge - Automatic Server Port Exposure Mod

**Make your Minecraft server publicly accessible without touching your router.**

PortBridge is a server-side Minecraft mod that automatically exposes your dedicated server to the internet using intelligent fallback strategies. It tries UPnP first, and if that fails, uses a built-in reverse tunnel system.

## ✨ Key Features

- **🎯 Automatic Setup** - Works instantly, no manual configuration needed
- **🔄 Intelligent Fallback** - UPnP → Tunnel when needed
- **🏗️ Self-Contained Tunnel** - No external dependencies, built-in tunnel implementation
- **🛡️ Server-Side Only** - No client mod needed
- **⚡ Async/Thread-Safe** - Never blocks your Minecraft server
- **🔍 Full Diagnostics** - Debug commands to troubleshoot any network setup
- **🎛️ Highly Configurable** - Fine-tune behavior for your network

## 📋 Supported Methods

### 1. UPnP/IGD (Primary)
- Works on most home networks
- Direct connection = best latency
- Automatic lease renewal

### 2. Built-In Reverse Tunnel (Fallback)
- Works on **any network** with outbound connectivity
- Corporate firewalls, ISP CGN, mobile hotspots
- Persistent connection to relay server
- Zero router configuration

**Choose AUTO (try both) or FORCE (tunnel only)**

## 🚀 Quick Start

### Installation

1. Download `portbridge-1.0.0.jar`
2. Drop into `mods/` folder
3. Restart server
4. Check console for public address

Example output:
```
[PortBridge] ========================================
[PortBridge] Server is publicly accessible!
[PortBridge] Method: UPnP
[PortBridge] Address: 203.0.113.42:25565
[PortBridge] ========================================
```

### Verify It Works

```
/portbridge status
```

Shows exposure status, method, and public address.

## 🎮 Commands

All commands require operator status (`/op <player>`)

| Command | Description |
|---------|-------------|
| `/portbridge status` | Show current exposure status |
| `/portbridge diag` | Show detailed diagnostics (tunnel metrics, session ID, etc.) |
| `/portbridge enable` | Re-enable exposure (if disabled) |
| `/portbridge disable` | Temporarily disable exposure |
| `/portbridge reload` | Reload config from disk |
| `/portbridge retry` | Force immediate retry of exposure methods |

## ⚙️ Configuration

Edit `config/portbridge-common.toml` after first server start.

### Basic Setup

```toml
# Enable UPnP
enablePortForwarding = true

# Your server port (must match server.properties)
internalPort = 25565
externalPort = 25565

# Enable tunnel as fallback
tunnel.enabled = true
tunnel.mode = "AUTO"  # or "FORCE" to always use tunnel
```

### Detailed Config Guide

See `CONFIGURATION.md` for:
- All 20+ configuration options explained
- Common scenarios and examples
- Performance tuning tips
- Self-hosted relay setup

## 🌐 How It Works

### UPnP Method (Fast)
1. Discovers router via SSDP
2. Maps external port to your server
3. Detects public IP
4. Prints: `YOUR_IP:25565`

**Best for:** Home networks with UPnP enabled

### Tunnel Method (Universal)
1. Opens persistent TCP connection to relay
2. Relay assigns public address
3. Player connections routed through tunnel to your server
4. Prints: `relay.portbridge.net:12345`

**Best for:** Corporate networks, mobile hotspots, ISP restrictions

### Automatic Fallback
```
UPnP fails → Auto-switch to tunnel → Continue running seamlessly
```

No restart needed. Transparent to players.

## 📊 Diagnostics

Run `/portbridge diag` to see:

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

## 🔒 Security

- **No credentials** needed - UUID-based session
- **No exposed data** - only relay knows server IP
- **Outbound only** - no inbound ports opened
- **Traffic safe** - relay forwards encrypted/unencrypted equally

## 🛠️ Troubleshooting

| Issue | Solution |
|-------|----------|
| UPnP not available | Enable on router, or use `tunnel.mode = "FORCE"` |
| Tunnel can't connect | Check firewall allows outbound TCP:7000 |
| Players can't join | Run `/portbridge diag` and check public address |
| Connection unstable | Enable `debugLogging = true` in config |
| Port already mapped | Delete old mapping from router admin panel |

Full troubleshooting guide in `PORTBRIDGE_GUIDE.md`

## 📋 Requirements

- **Minecraft:** 1.21.1 (1.20+ architecture compatible)
- **Mod Loader:** NeoForge 21.1.216
- **Java:** 21+
- **Server Type:** Dedicated server only (no client mod)

## 🏗️ Architecture

```
PortBridge (Main)
├── ExposureManager (orchestrates methods)
├── UPnPExposureService
│   ├── SimpleUPnP (SSDP discovery)
│   ├── PublicIPResolver
│   └── Lease Renewal
└── TunnelExposureService
    ├── TunnelClient (TCP socket)
    ├── TunnelSession (state tracking)
    ├── TunnelForwarder (traffic relay)
    └── Auto-reconnect (exponential backoff)
```

Each service runs in dedicated executor threads. Server main thread never blocked.

## 📈 Performance

- Memory: ~2-5 MB for idle tunnel
- CPU: Minimal, mostly I/O-bound
- Network: Only keepalive + relay forwarding
- Concurrent Players: No limits, scales with server

## 📚 Documentation

- **PORTBRIDGE_GUIDE.md** - Complete feature overview, how it works, troubleshooting
- **CONFIGURATION.md** - Every config option explained with examples
- **This README** - Quick start and command reference

## 🔄 Version History

### 1.0.0 (Release)
- ✅ UPnP/IGD port forwarding
- ✅ Built-in reverse tunnel system
- ✅ Automatic fallback strategy (UPnP → Tunnel)
- ✅ `/portbridge` command suite with diagnostics
- ✅ Comprehensive configuration
- ✅ Proper async threading + clean shutdown
- ✅ Full documentation

## 📝 License

Licensed under TEMPLATE_LICENSE.txt

## 🙋 Support

**Before posting issues, run:**
```
/portbridge diag
/portbridge status
```

And check `config/portbridge-common.toml` for obvious issues.

---

**Made for Minecraft server administrators who just want it to work.** 🎮
