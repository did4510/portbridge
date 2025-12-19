# PortBridge Mod - File-by-File Copilot Prompts

These are ready-to-use prompts for GitHub Copilot's inline chat mode. Each prompt is designed to generate one specific file.

## 1. Config.java

```
Create a NeoForge mod configuration class for PortBridge with these options:
- enablePortForwarding (boolean, default true)
- internalPort (int, default 25565)
- externalPort (int, default 25565)
- leaseDuration (int, default 3600 seconds)
- refreshInterval (int, default 1800 seconds)
- retryCount (int, default 3)
- retryDelay (int, default 5 seconds)
- enablePublicIPFallback (boolean, default false)
- publicIPFallbackURL (string, default "https://api.ipify.org")
- enableOperatorBroadcast (boolean, default true)
- debugLogging (boolean, default false)

Use ModConfigSpec.Builder and ensure config reload support with @EventBusSubscriber.
```

## 2. SimpleUPnP.java

```
Create a SimpleUPnP utility class for Minecraft mod that implements UPnP port forwarding:

Features needed:
- SSDP discovery of Internet Gateway Devices
- XML parsing of service descriptions
- SOAP requests for:
  - AddPortMapping (TCP port forwarding)
  - DeletePortMapping
  - GetSpecificPortMappingEntry (check if mapped)
  - GetExternalIPAddress
- Timeout handling (5 second timeouts)
- Error handling without crashing

Methods:
- isUPnPAvailable(): boolean
- openPortTCP(externalPort, internalPort, internalIP, description, lease): boolean
- closePortTCP(externalPort): boolean
- isMappedTCP(externalPort): boolean
- getExternalIP(): String

Use standard Java networking, no external libraries.
```

## 3. PortForwardingManager.java

```
Create a PortForwardingManager class for NeoForge mod that handles UPnP port forwarding asynchronously:

Requirements:
- Background thread execution (ScheduledExecutorService)
- Attempt port forwarding on startup with retries
- Automatic lease renewal every refreshInterval
- Track mapping status (active/error)
- Clean shutdown on server stop
- Integration with Config class for settings

Methods:
- startPortForwarding(): void
- stopPortForwarding(): void
- retry(): void
- isActive(): boolean
- getExternalPort(): int
- getLastError(): String

Handle UPnP failures gracefully, log appropriately.
```

## 4. PublicIPResolver.java

```
Create a PublicIPResolver class for NeoForge mod that detects public IP address:

Logic:
1. Primary: Get external IP from UPnP router
2. Fallback: HTTP GET to configurable URL (if enabled)
3. Validate IP format (IPv4/IPv6 regex)
4. Handle timeouts (5 seconds) and errors

Methods:
- getPublicIP(): CompletableFuture<String>

Use SimpleUPnP for UPnP method, standard HttpURLConnection for fallback.
Async execution, return future for non-blocking.
```

## 5. CommandHandler.java

```
Create a CommandHandler class for NeoForge mod with /portbridge command:

Requirements:
- Operator-only permission (level 2)
- Subcommands: status, retry, enable, disable, reload
- Asynchronous execution (no main thread blocking)
- Integration with PortBridge instance

Command behaviors:
- status: Show active state, ports, public IP, last error
- retry: Trigger immediate port forwarding retry
- enable: Enable forwarding and start
- disable: Disable and remove mapping
- reload: Reload config from disk

Use @EventBusSubscriber for RegisterCommandsEvent.
```

## 6. PortBridge.java (Main Class)

```
Refactor the main PortBridge mod class for server-side port forwarding:

Remove all template code (blocks, items, tabs).
Add server lifecycle handling:
- @SubscribeEvent on ServerStartedEvent: Initialize managers asynchronously
- @SubscribeEvent on ServerStoppingEvent: Clean shutdown

Logic:
- Wait for server full start
- Start port forwarding in background
- After forwarding attempt, resolve public IP
- Print join address to console
- Optionally broadcast to operators
- Handle all operations asynchronously

Integrate: Config, PortForwardingManager, PublicIPResolver, CommandHandler.
```

## 7. PortBridgeClient.java

```
Simplify PortBridgeClient.java for server-only mod:

Remove all client-side code and logic.
Keep minimal class structure for NeoForge compatibility.
Add comment that mod is server-side only.
```

## 8. build.gradle (Dependency)

```
Update build.gradle for PortBridge mod:

Add Maven Central repository if not present.
No external dependencies needed (using built-in Java networking).
Ensure Java 21 toolchain.
Comment on any UPnP library alternatives if needed.
```

## Usage Instructions

For each file:
1. Open the file in VS Code
2. Use GitHub Copilot Chat or Inline Chat
3. Paste the corresponding prompt
4. Review and adjust generated code
5. Test compilation

This approach allows incremental development and easier debugging.