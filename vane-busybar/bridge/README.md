# BarMC bridge

Shows your Minecraft server on a [BUSY Bar](https://busy.bar). It connects to the `vane-busybar`
plugin, draws on the Bar through the official [busylib](https://pypi.org/project/busylib/) SDK,
and sends the Bar's controls back to the server.

```
Paper + vane-busybar ──events──▶ barmc (this) ──local HTTP──▶ BUSY Bar
                     ◀─requests──              ◀─buttons────
```

Run it on a computer on the same network as the Bar (or with the Bar plugged in over USB).
It only makes outgoing connections, so it works behind a home router.

The bridge lives in the vane repository next to the plugin, `vane-busybar/bridge`, and is
released with the same version: bridge 1.23 goes with vane-busybar 1.23. Each GitHub release has
it as `barmc-<version>.zip` (also built into `target/` by `./gradlew build`), `.tar.gz` and a
wheel, each installable with `pip install`.

## Setup

Needs Python 3.10 or newer. From a release:

```sh
python3 -m venv ~/.local/share/barmc
~/.local/share/barmc/bin/pip install barmc-<version>-py3-none-any.whl
```

Or from a checkout of the vane repository:

```sh
cd vane-busybar/bridge
python3 -m venv .venv
.venv/bin/pip install -e .
```

The same on Windows, in PowerShell:

```powershell
cd vane-busybar\bridge
py -m venv .venv
.venv\Scripts\pip install -e .
```

macOS works like Linux, with a current Python (see [macOS](#macos)). For a release on Windows,
see [Windows](#windows).

## Try it with the vane dev server

1. Start the server:

   ```sh
   # from the root of the vane repository
   ./gradlew runServer --no-daemon --warning-mode all
   ```

   On Windows, run `.\gradlew.bat runServer --no-daemon --warning-mode all` instead.

2. Join `localhost` in Minecraft and run `/busybar link`. It prints a pairing string such as
   `<token>@<ip>:9123#sha256=<fingerprint>`. Click it to copy it; it is shown only once.

3. Preview in the terminal, no Bar needed:

   ```sh
   .venv/bin/barmc --pair '<pairing string>' --terminal
   ```

   Keys: `o`/Enter = OK, `x`/Backspace = BACK, `[` `]` or arrows = wheel, `b` = move the
   selector to or from BUSY.

4. On the real Bar:

   ```sh
   # USB
   .venv/bin/barmc --pair '<pairing string>' --bar 10.0.4.20
   # Wi-Fi: the Bar's IP and the API password set in its web interface
   .venv/bin/barmc --pair '<pairing string>' --bar 192.168.1.20 --bar-token 1234
   ```

   On Windows, the program is `.venv\Scripts\barmc` and the rest stays the same. In `cmd`, put
   the pairing string in double quotes; PowerShell takes either.

Options can also come from the environment: `BARMC_PAIR`, `BUSYBAR_ADDR`, `BUSYBAR_TOKEN`
(and `BARMC_SERVER`, `BARMC_TOKEN`, see below). Add `-v` to log every event.

## Use it on a public server

### Players

Join the server, run, and start the bridge with the pairing string it shows, as in
steps 3 and 4 above, on Linux, macOS, or Windows alike. If it says the server has no address for bridges yet, ask an admin to follow
the next section. Keep the bridge running for a permanent setup as described in
[Security](#security), [macOS](#macos) or [Windows](#windows).

### Server admins

Bridges connect to the plugin's own port, TCP 9123 by default, not to the Minecraft port. With
the default `Tls: auto` there is nothing to buy or renew: the plugin makes its own certificate and
`/busybar link` hands each player its fingerprint.

1. Open the port to the internet: allow TCP 9123 in the server's firewall and, at home, forward
   it on the router to the server. On a hosting panel that assigns ports, add an extra port
   allocation and put its number in `Port`.

   - Linux: `ufw allow 9123/tcp`, or `firewall-cmd --add-port=9123/tcp --permanent` followed by
     `firewall-cmd --reload`.
   - Windows, in PowerShell as administrator:
     `New-NetFirewallRule -DisplayName "vane-busybar" -Direction Inbound -Protocol TCP -LocalPort 9123 -Action Allow`.
     Windows may also ask to allow Java on the first start; allow it for the networks players come
     from.
   - macOS: the firewall is off by default. If it is on, allow incoming connections for `java`
     when asked or add it under System Settings > Network > Firewall > Options.
2. Tell the plugin the address players reach it at. In `plugins/vane-busybar/config.yml`, set
   `PublicUrl` to the server's public domain or IP, the same one players join in Minecraft:

   ```yaml
   PublicUrl: "mc.example.com"   # Port is added: mc.example.com:9123
   Port: 9123
   Tls: "auto"
   ```

   Without it, the plugin uses `server-ip` from `server.properties`, which is often empty or an
   internal address; with neither, `/busybar link` gives out no token. If the hosting panel maps
   the port to a different public one, write it out: `PublicUrl: "mc.example.com:25570"`.
3. Restart the server, then check from a computer outside its network:

   ```sh
   curl -k -o /dev/null -w '%{http_code}\n' https://mc.example.com:9123/events
   ```

   macOS has the same `curl`. On Windows 10 and 11, use `curl.exe` in PowerShell, since plain
   `curl` there means a different command:

   ```powershell
   curl.exe -k -o NUL -w "%{http_code}" https://mc.example.com:9123/events
   ```

   `401` means the port is reachable and the plugin answered; a timeout means the port is still
   closed or forwarded wrongly. (`-k` only skips the certificate check for this test; the bridge
   checks the fingerprint.)

- **Online mode.** The fingerprint reaches players through Minecraft's encrypted connection. On
  an offline-mode server that connection is not encrypted, so someone on the way could swap the
  pairing string; the plugin warns about this on startup.
- **What players may see and do** is set by the `vane.busybar.*` permissions and
  `AllowedActions`; TPS, autostop, and region visitor alerts are for ops only by default. See the
  plugin's [Module.md](../Module.md) for the full list.
- **Revoking.** `/busybar revoke <player>` disconnects a player's bridge, `/busybar rotatekey`
  replaces the key if `plugins/vane-busybar/tls-auto.key` ever leaked, after which every player
  has to link again. Back up the `plugins/vane-busybar` folder with the server: it holds the key
  and the players' tokens, so players stay linked after a restore. On Linux and macOS the plugin
  makes `tls-auto.key` readable by the server's user only; on Windows the file inherits the
  folder's permissions, so on a shared Windows machine keep the server folder private to the
  account running the server.
- **A domain with a real certificate** (`Tls: keystore`) or **a reverse proxy** in front of the
  port (`Tls: off`, `BindAddress: "127.0.0.1"`, `PublicUrl` set to the proxy's `https://` URL) work
  too; see TLS in [Module.md](../Module.md). Neither is needed for a public server.
- **Behind a Velocity proxy** with vane-velocity, the proxy can serve the bridges for the whole
  network instead, so a player's Bar follows them from server to server. Players then run
  `/busybar link` on the proxy, and the proxy's port 9123 is the one to open. See "Behind a
  Velocity proxy" in [Module.md](../Module.md).

## Security

The pairing string holds everything the bridge needs: the server address, your token, and the
fingerprint of the server's TLS key. The bridge only talks to a server presenting that exact key,
and checks it before sending anything, so the token never reaches an impostor. This works without
a domain or a certificate authority; the server's certificate is self-signed on purpose.

Treat the pairing string like a password: anyone holding it can read your events and switch your
focus mode on that server (not log into Minecraft as you). Command-line arguments are visible to
every user of the machine and land in your shell history, so for a permanent setup keep it in a
file only you can read:

```sh
install -m 600 /dev/null ~/.config/barmc.env
cat > ~/.config/barmc.env <<'ENV'
BARMC_PAIR='<token>@<ip>:9123#sha256=<fingerprint>'
BUSYBAR_ADDR=192.168.1.20
BUSYBAR_TOKEN=1234
ENV
```

And run it as a systemd user service, `~/.config/systemd/user/barmc.service`. Replace the
`ExecStart` path with your own: `~/.local/share/barmc/bin/barmc` for a release install, or what
`realpath .venv/bin/barmc` prints inside `vane-busybar/bridge` for a checkout:

```ini
[Unit]
Description=BarMC bridge
After=network-online.target

[Service]
EnvironmentFile=%h/.config/barmc.env
ExecStart=/home/you/vane/vane-busybar/bridge/.venv/bin/barmc
Restart=on-failure
RestartPreventExitStatus=2 3

[Install]
WantedBy=default.target
```

```sh
systemctl --user enable --now barmc
loginctl enable-linger "$USER"   # keep it running while logged out, e.g. on a Raspberry Pi
```

When the bridge stops on its own, its exit code says why:

| Exit | Meaning                                                                                | Fix                                                                                                     |
|------|----------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------|
| 2    | The server rejected the token: it was revoked, or replaced by a newer `/busybar link`. | Run `/busybar link` and update the pairing string.                                                      |
| 3    | The server's TLS key does not match the pairing string. Nothing was sent to it.        | If an admin ran `/busybar rotatekey`, link again. Otherwise someone may be intercepting the connection. |

Other connection problems are retried with backoff. `RestartPreventExitStatus` keeps systemd from
retrying 2 and 3, which only a new pairing string fixes.

**Servers without the fingerprint.** A pairing string without `#sha256=` means the admin set the
plugin's `Tls` to `keystore` (a certificate your system already trusts, validated as usual) or
`off`. With `off`, the string starts with `http://` and the bridge warns if the server is not on
your local network: the token then crosses the internet unencrypted. `--server` and `--token`
(`BARMC_SERVER`, `BARMC_TOKEN`) remain for setting the two parts separately.

## macOS

The Python that comes with the Xcode command line tools is 3.9, too old. Install a current one
with Homebrew (`brew install python`) or from python.org, then:

```sh
python3 -m venv ~/.local/share/barmc
~/.local/share/barmc/bin/pip install barmc-<version>-py3-none-any.whl
```

Running it, and the private env file works as on Linux (see [Security](#security)): put
`BARMC_PAIR`, `BUSYBAR_ADDR` and `BUSYBAR_TOKEN` in `~/.config/barmc.env`, readable only by you.

The preview uses 256 colors in Terminal.app, which garbles 24-bit color before macOS 26, and
24-bit color in iTerm2, Ghostty, and other terminals. `--colors 24bit` or `--colors 256` overrides
the choice.

To start the bridge at login, save this as `~/Library/LaunchAgents/org.oddlama.vane.barmc.plist`,
with your username in the log path, since launchd does not expand `~` there:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "https://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist>
    <dict>
        <key>Label</key>
        <string>org.oddlama.vane.barmc</string>
        <key>ProgramArguments</key>
        <array>
            <string>/bin/sh</string>
            <string>-c</string>
            <string>set -a; . "$HOME/.config/barmc.env"; exec "$HOME/.local/share/barmc/bin/barmc"</string>
        </array>
        <key>RunAtLoad</key>
        <true/>
        <!-- Restart after a crash, but not after exit codes 2 and 3, which need a new pairing string. -->
        <key>KeepAlive</key>
        <dict>
            <key>Crashed</key>
            <true/>
        </dict>
        <key>StandardErrorPath</key>
        <string>/Users/you/Library/Logs/barmc.log</string>
    </dict>
</plist>
```

```sh
launchctl bootstrap gui/$(id -u) ~/Library/LaunchAgents/org.oddlama.vane.barmc.plist   # start now and at login
launchctl bootout gui/$(id -u)/org.oddlama.vane.barmc                                   # stop and disable
```

Its messages end up in `~/Library/Logs/barmc.log`, which Console.app also shows. Like on Windows,
it reconnects by itself and only stops for exit codes 2 and 3, so it is not restarted for those.

## Windows

Install Python from python.org (tick "Add python.exe to PATH"), then in PowerShell:

```powershell
py -m venv $env:LOCALAPPDATA\barmc
& $env:LOCALAPPDATA\barmc\Scripts\pip install barmc-<version>-py3-none-any.whl
```

Try it with the preview first. Use double quotes around the pairing string in `cmd`; PowerShell
takes either:

```powershell
& $env:LOCALAPPDATA\barmc\Scripts\barmc --pair "<token>@<ip>:9123#sha256=<fingerprint>" --terminal
```

The preview looks best in Windows Terminal, the default on Windows 11; the older console works
too. The keys are the same as on Linux.

For a permanent setup, keep the pairing string and the Bar's password out of the command line by
storing them in your user environment, which other users of the PC cannot read:

```powershell
setx BARMC_PAIR "<token>@<ip>:9123#sha256=<fingerprint>"
setx BUSYBAR_ADDR "192.168.1.20"
setx BUSYBAR_TOKEN "1234"
```

`setx` only affects programs started afterward, so open a new window before testing.

To start the bridge when you log in, register it with Task Scheduler, the Windows counterpart of
the systemd service above. In a new PowerShell window, after `setx`:

```powershell
$barmc    = "$env:LOCALAPPDATA\barmc\Scripts"
$action   = New-ScheduledTaskAction -Execute "$barmc\pythonw.exe" -Argument "-m barmc"
$trigger  = New-ScheduledTaskTrigger -AtLogOn -User $env:USERNAME
$settings = New-ScheduledTaskSettingsSet -ExecutionTimeLimit ([TimeSpan]::Zero) `
            -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
Register-ScheduledTask -TaskName barmc -Action $action -Trigger $trigger -Settings $settings
Start-ScheduledTask barmc
```

`pythonw.exe` runs the bridge without a console window, so it reads its settings from the `setx`
variables and keeps no log. The zero `ExecutionTimeLimit` stops Task Scheduler from ending it
after three days, its default. If registering says access is denied, run PowerShell as
administrator once; the task still runs as you.

```powershell
Stop-ScheduledTask barmc                          # stop it
Start-ScheduledTask barmc                         # start it again, e.g. after a new setx
(Get-ScheduledTaskInfo barmc).LastTaskResult      # exit code of the last run, 2 or 3 see above
Unregister-ScheduledTask barmc -Confirm:$false    # remove it
```

There is no need to restart it on failure: it reconnects by itself after network problems and
server restarts, and only stops for exit codes 2 and 3 above, which a new pairing string fixes.
For the same reason, leave the task's restart-on-failure setting off; it cannot skip those two.
To see what it is doing, stop the task and run `barmc` in a window as above, with `-v`.

To run it on an always-on PC before anyone logs in, like `loginctl enable-linger` on Linux, wrap
`barmc.exe` in a Windows service with [NSSM](https://nssm.cc) or
[WinSW](https://github.com/winsw/winsw). A service's settings are readable by the PC's
administrators, so only do that on a machine you trust them with.

## What the Bar shows

Idle screens rotate every 5 seconds; the wheel flips through them.

- **Players**: the server icon, players online and TPS (TPS only for players with
  `vane.busybar.receive.server`).
- **World clock**: in-game time in 10-minute steps, day or night.
- **Bedtime**: who is asleep and how many more are needed, while anyone is.
- **Autostop**: a countdown while the empty server is about to stop. With
  `vane.busybar.action.autostop_abort`, OK cancels it.
- **Region** (with vane-regions): the region you are standing in, and whether it is your land, a
  safe zone without PvP, or whose it is.

**Visitors** (with vane-regions and `vane.busybar.receive.region_visitors`, ops only by
default): when a stranger walks into one of your regions, the Bar shows their face and where, even
while you are offline. Several visitors show as `ALEX +2`, a crowd as `7 VISITORS`; the server
reports a region's first visitor at once and the rest at most once a minute.

Events such as joins, portals, night skips, and lag appear as alerts on top, most important first.
BACK dismisses one. Join and leave alerts show the player's own face from their skin, at 16x16;
on offline-mode servers, or until the face has downloaded, a generic head stands in.

**Focus mode**: move the selector to BUSY. The server tags you `[Busy]` in the tab list, the Bar
shows a 25-minute focus timer, and only critical alerts (autostop, server stop) get through.
Moving the selector away shows how many alerts were held.

## Not verified on a device yet

The bridge has been tested against the vane dev server and a fake Bar that records requests, not
a real BUSY Bar. Things to check on the device:

- Text placement uses busylib's own two-line layout (`small` font, top line at y=0, bottom
  line anchored to y=16). Tune `TEXT_X`, `FONT` and the y values in
  `barmc/display/busybar.py` if lines clip.
- Lines longer than about nine characters scroll, following busylib's rule of thumb. Short lines
  like `TIME 07:30` may fit without scrolling on the device.
- Whether the Bar keeps showing the bridge's screen when the selector is on BUSY, since the
  firmware has its own busy screen. `--priority` (1-100) sets how strongly the bridge claims
  the display.
- On macOS: the tests run there in CI, but the LaunchAgent and Terminal.app's colors have not
  been tried on a real Mac. The Bar's USB connection likely works without a driver, since macOS
  supports USB network adapters out of the box, but is untested too.
- On Windows: the tests run there in CI, but the preview's keys, colors, and Ctrl+C have only
  been simulated, not used on a real Windows PC. Whether Windows sets up the Bar's USB network
  connection (`--bar 10.0.4.20`) without a driver is also open; Wi-Fi does not depend on it.
  The Task Scheduler setup and running under `pythonw.exe` have not been tried there either.

## Development

```sh
.venv/bin/pip install -e '.[test]'
.venv/bin/python -m pytest
```

- `barmc/state.py`: screens, alert priorities, focus mode, and inputs. Display-independent.
- `barmc/server.py`: the plugin's event stream, requests, and server icon.
- `barmc/pairing.py`: pairing strings and pinning of the server's TLS key.
- `barmc/display/busybar.py`: drawing on the BUSY Bar and reading its controls.
- `barmc/display/terminal.py`: the terminal preview.
- `barmc/sprites.py`, `barmc/icons.py`: pixel art and icon conversion.
