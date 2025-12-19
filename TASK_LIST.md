# PortBridge Mod - Task List for GitHub Copilot Inline Coding

This is a breakdown of the PortBridge mod implementation into actionable tasks for GitHub Copilot's inline coding mode. Each task focuses on a specific file or component.

## Phase 1: Core Setup

### Task 1.1: Update Main Mod Class (PortBridge.java)
- Remove template code (blocks, items, tabs)
- Add server lifecycle event handlers (ServerStartedEvent, ServerStoppingEvent)
- Initialize managers in background threads
- Handle public IP resolution and console output
- Implement operator broadcast logic

### Task 1.2: Create Configuration System (Config.java)
- Replace template config with PortBridge-specific options
- Add all required config values:
  - enablePortForwarding (boolean)
  - internalPort, externalPort (int)
  - leaseDuration, refreshInterval (int)
  - retryCount, retryDelay (int)
  - enablePublicIPFallback (boolean)
  - publicIPFallbackURL (string)
  - enableOperatorBroadcast (boolean)
  - debugLogging (boolean)
- Ensure config reload support

## Phase 2: Port Forwarding Implementation

### Task 2.1: Create SimpleUPnP Utility Class
- Implement UPnP device discovery (SSDP M-SEARCH)
- Parse XML service descriptions
- Create SOAP request methods for:
  - AddPortMapping
  - DeletePortMapping
  - GetSpecificPortMappingEntry
  - GetExternalIPAddress
- Handle timeouts and errors gracefully

### Task 2.2: Create PortForwardingManager Class
- Initialize with background executor
- Implement port forwarding attempt logic
- Add automatic lease renewal scheduling
- Handle retry logic with configurable attempts
- Provide status tracking (active/error state)
- Clean shutdown on server stop

## Phase 3: IP Detection

### Task 3.1: Create PublicIPResolver Class
- Implement UPnP-based IP detection (primary)
- Add HTTP fallback with configurable URL
- Validate IP address format (IPv4/IPv6)
- Handle timeouts and network errors
- Return CompletableFuture for async operation

## Phase 4: User Interface

### Task 4.1: Create Command Handler
- Register /portbridge command with operator permission
- Implement subcommands:
  - status: Show mapping state, ports, IP, errors
  - retry: Trigger immediate port forwarding retry
  - enable: Enable and start port forwarding
  - disable: Disable and remove mapping
  - reload: Reload configuration
- Ensure all operations are asynchronous

### Task 4.2: Clean Up Client Class
- Remove unnecessary client-side code
- Keep minimal or empty implementation since mod is server-only

## Phase 5: Integration & Testing

### Task 5.1: Wire Everything Together
- Connect managers in main mod class
- Ensure proper initialization order
- Add comprehensive logging
- Handle edge cases (no UPnP, network errors, etc.)

### Task 5.2: Error Handling & Stability
- Add try-catch blocks throughout
- Implement graceful degradation
- Ensure server never crashes
- Add debug logging options

## File Structure Checklist

- [ ] `src/main/java/com/darsh/portbridge/PortBridge.java` - Main mod class
- [ ] `src/main/java/com/darsh/portbridge/Config.java` - Configuration
- [ ] `src/main/java/com/darsh/portbridge/SimpleUPnP.java` - UPnP utilities
- [ ] `src/main/java/com/darsh/portbridge/PortForwardingManager.java` - Port forwarding logic
- [ ] `src/main/java/com/darsh/portbridge/PublicIPResolver.java` - IP detection
- [ ] `src/main/java/com/darsh/portbridge/CommandHandler.java` - Commands
- [ ] `src/main/java/com/darsh/portbridge/PortBridgeClient.java` - Client stub
- [ ] `build.gradle` - Dependencies (add Maven Central if needed)
- [ ] `README.md` - Documentation

## Testing Checklist

- [ ] Server starts without errors
- [ ] Config file generates correctly
- [ ] Port forwarding attempts on startup
- [ ] Console shows appropriate success/failure messages
- [ ] Commands work for operators
- [ ] No main thread blocking
- [ ] Graceful handling of UPnP failures
- [ ] Public IP detection works
- [ ] Operator broadcasts function (when enabled)

## Copilot Inline Prompts

For each task, use prompts like:

"Implement the PortForwardingManager class for automatic UPnP port forwarding with lease renewal"

"Create the SimpleUPnP utility class with SSDP discovery and SOAP requests"

"Add the /portbridge command system with status, retry, enable, disable, and reload subcommands"

This breakdown allows for incremental implementation and easier debugging.