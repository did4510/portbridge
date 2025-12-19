
# PortBridge - Minecraft Server Port Forwarding Mod

A server-side Minecraft mod that automatically forwards the server port and displays the public join address, making your server publicly accessible without manual router configuration.

## Status: ✅ COMPLETE & READY

The PortBridge mod has been fully implemented and built. The JAR file is available at `build/libs/portbridge-1.0.0.jar`.

## Overview

PortBridge automatically:
- Discovers your network's UPnP-capable router
- Creates a port forwarding rule for your Minecraft server
- Detects your public IP address
- Prints the complete join address (IP:port) in the server console
- Optionally notifies operators in-game

## Features

### Core Functionality
- **Automatic Port Forwarding**: Sets up port forwarding on server startup
- **Public IP Detection**: Retrieves external IP from router or fallback service
- **Console Output**: Displays joinable address like `203.0.113.10:25565`
- **Operator Notifications**: Optional in-game messages for server operators
- **Lease Management**: Automatically renews port mappings to prevent expiration

### Configuration
- Enable/disable port forwarding
- Configure internal and external ports
- Set lease duration and refresh intervals
- Control retry attempts and delays
- Toggle public IP fallback (privacy-aware)
- Enable/disable operator broadcasts
- Debug logging options

### Commands
```
/portbridge status    # Show current mapping status
/portbridge retry     # Retry port forwarding
/portbridge enable    # Enable port forwarding
/portbridge disable   # Disable and remove mapping
/portbridge reload    # Reload configuration
```

## Installation

1. Download the `portbridge-1.0.0.jar` file from `build/libs/`
2. Place it in your server's `mods` folder
3. Start the server - configuration will be generated automatically
4. Edit `config/portbridge-common.toml` as needed
5. Restart server or use `/portbridge reload`

## Configuration

The mod creates a configuration file at `config/portbridge-common.toml`:

```toml
# Enable automatic port forwarding
enablePortForwarding = true

# Server ports
internalPort = 25565
externalPort = 25565

# Port mapping lease settings
leaseDuration = 3600
refreshInterval = 1800

# Retry settings
retryCount = 3
retryDelay = 5

# Public IP detection
enablePublicIPFallback = false
publicIPFallbackURL = "https://api.ipify.org"

# Notifications
enableOperatorBroadcast = true

# Debugging
debugLogging = false
```

## How It Works

### Server Startup Process
1. Server fully initializes
2. Background thread starts network discovery
3. Attempts UPnP device discovery
4. Creates TCP port mapping (external → internal)
5. Retrieves public IP address
6. Prints join address to console
7. Optionally broadcasts to operators

### Port Forwarding Logic
- Discovers UPnP Internet Gateway Device
- Locates WANIPConnection or WANPPPConnection service
- Requests port mapping with specified parameters
- Handles failures gracefully without crashing server

### Public IP Detection
1. **Primary**: Query router's external IP via UPnP
2. **Fallback**: HTTPS request to configurable endpoint (if enabled)
3. **Validation**: Ensures valid IPv4/IPv6 format

## Console Output Examples

### Success
```
[PortBridge] Port forwarding active
[PortBridge] External port: 25565
[PortBridge] Server join address: 203.0.113.10:25565
```

### Failure
```
[PortBridge] Port forwarding failed or unsupported
[PortBridge] Server may not be publicly reachable
[PortBridge] Manual port forwarding may be required
```

## Requirements

- Minecraft server with NeoForge/Forge
- UPnP-capable router (most modern routers support this)
- No client-side installation required

## Safety & Security

- Only modifies port forwarding rules
- No external requests by default (configurable fallback)
- Never exposes sensitive router information
- Safe for home network use
- Graceful failure handling

## Troubleshooting

### Port Forwarding Fails
- Check if your router supports UPnP
- Ensure UPnP is enabled in router settings
- Try manual port forwarding as alternative
- Use `/portbridge retry` to attempt again

### Public IP Not Detected
- Enable public IP fallback in config
- Check network connectivity
- Verify fallback URL is accessible

### Commands Not Working
- Ensure you have operator permissions
- Check server console for errors
- Verify mod is loaded correctly

## Technical Details

### Architecture
- **Main Mod Class**: Lifecycle management and orchestration
- **PortForwardingManager**: UPnP networking and mapping logic
- **PublicIPResolver**: IP detection and validation
- **Config Manager**: Settings persistence and reloading
- **Command Handler**: User interaction and status reporting

### Threading
- All network operations run asynchronously
- No blocking of main server thread
- Proper cleanup on server shutdown
- Background lease renewal

### Error Handling
- Comprehensive exception catching
- Detailed logging with configurable verbosity
- Non-disruptive failure modes
- Automatic retry mechanisms


## License

This project is licensed under the MIT License. See the `LICENSE` file at the repository root for details.

Replace the `LICENSE` file or `gradle.properties` settings if you wish to use a different license. Ensure `gradle.properties` `mod_license` matches your chosen license identifier before publishing.

Authorship
 `Darsh(https://github.com/did4510)`

## Support

For issues or questions:
- Check the troubleshooting section
- Review server logs for detailed error messages
- Ensure UPnP is enabled on your router
- Consider manual port forwarding if UPnP fails
