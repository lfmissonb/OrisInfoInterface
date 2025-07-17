# Oris Info Interface

A Minecraft plugin provides interfaces to get server infos.  
🎉**This is my very first open-source project! - your feedback is priceless!**

## Config

```yaml
# ----------------------------------
# General Settings
# ----------------------------------
config-version: 1         # Configuration version. DO NOT MODIFY.
plugin-enabled: false     # Master switch. If false, the entire plugin is disabled regardless of module settings.
debug-mode: false

# ----------------------------------
# API Server Settings
# ----------------------------------
api:
  host: 127.0.0.1         # The IP address the API server will bind to. Use 0.0.0.0 to listen on all interfaces.
  port: 25000             # The port on which the API server listens.
  log-requests: true      # If true, the request will be recorded as follows: [127.0.0.1] [GET /version HTTP/1.1 200] User-Agent

# ----------------------------------
# Module Settings
# ----------------------------------

player:
  enabled: true           # Enables the /info.player API output. If false, both current and history are disabled.
  history:
    enabled: true         # Whether to record historical player count data.
    retention-days: 30    # Number of days to retain data. Set to 0 or a negative value to keep history forever.

tps:
  enabled: true
  history:
    enabled: true
    retention-days: 30
    refresh-delay: 60      # Refresh delay (in seconds)

mspt:
  enabled: true
  history:
    enabled: true
    retention-days: 30
    refresh-delay: 60

```

## API Usage

**GET /version** get the plugin version

```json
{
  "version": "1.1"
}
```

**GET /info** get the infos

```json
{
  "tps": {
    "current": 20.00,
    // float, -1 if disabled
    "history": {
      "<timestamp>": 19.00
      // Historical TPS (Unix timestamp(str) in seconds → value)
    }
  },
  "mspt": {
    "current": 50.00,
    // float, -1 if disabled
    "history": {
      "<timestamp>": 50.00
    }
  },
  "player": {
    "current": 10,
    // int, -1 if disabled
    "history": {
      "<timestamp>": 20
    }
  }
}
```
