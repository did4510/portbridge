PortBridge - README

What it does
- Automatically attempts UPnP/IGD port forwarding when the Minecraft server has fully started.
- Can map the main server port and additional ports (RCON, Query, etc.).
- Dry-run mode to simulate behavior without performing actual UPnP calls.
- Diagnostics via `/portbridge diag` (OP-only).

When it runs
- On server started event. It will discover UPnP IGD devices and attempt mapping.
- On server stopping, it attempts to remove mappings.

Configuration
- `enablePortForwarding` : Enable/disable the mod globally.
- `internalPort` / `externalPort` : Main port mapping values.
- `additionalPorts` : Comma separated entries `name:internal:external:protocol:enabled`. Example:
  `rcon:25575:25575:TCP:true,query:25565:25565:UDP:false`
- `allowedSubnets` : Comma separated CIDR ranges to restrict which local IPs are allowed for UPnP mapping (default `192.168.0.0/16,10.0.0.0/8`).
- `advanced.dryRun` : When true, no real UPnP calls are made; actions are simulated and logged.
- `operatorMessageTemplate` : Message template sent to OPs after a successful mapping. Supported placeholders: `{public_ip}`, `{port}`, `{protocol}`, `{world}`.

Limitations
- This implementation uses a simple SSDP discovery and SOAP interaction (no external UPnP library). Some routers have non-standard implementations and may not be discovered.
- UDP mapping is not fully implemented (treated as TCP placeholder) — add UDP SOAP payloads if required.
- Per-profile/per-world overrides must be done manually by editing the config (the mod supports per-world message placeholders but not automatic profile config files in this version).

Router compatibility notes
- Supports IGD devices implementing `WANIPConnection` or `WANPPPConnection` services.
- Some consumer routers respond slowly; the discovery has a short timeout (3s) and may need tweaking.
- If UPnP is disabled on the router/network, the mod will fail gracefully and (if enabled) fall back to tunnel mode (if configured).

Security
- The mod only attempts mappings when the detected local IP is inside one of the allowed subnets. This prevents accidental exposure when the server runs on an unexpected interface.
- Dry-run mode lets operators validate behavior on restrictive networks.

How it works (high-level)
- On `ServerStartedEvent`, the `ExposureManager` selects the desired method (UPnP primary, tunnel fallback if configured).
- `UPnPExposureService` discovers an IGD, determines the local LAN IP, checks allowed subnets, and requests port mappings via SOAP.
- A `PublicIPResolver` attempts to fetch the public IP via IGD or fallback service.
- Mappings are renewed on a scheduler before lease expiry.

Runtime commands
- `/portbridge status` - show current status and public address
- `/portbridge diag` - OP-only, show diagnostic info
- `/portbridge retry` - retry exposure methods
- `/portbridge enable` - enable exposure
- `/portbridge disable` - disable exposure
- `/portbridge reload` - reload config

If you want, I can:
- Add full UDP SOAP payloads and explicit UDP mapping support
- Add per-world/config file overrides
- Improve discovery timeouts and retries
- Add unit tests for CIDR parsing and dry-run behavior

