# DigitalOcean Deployment Kit

Deployment guide for running WiFi Chat server on a DigitalOcean Droplet, then testing from local client/admin app.

## Included files

- `deploy-to-droplet.ps1`: deploy from local Windows machine
- `provision-server.sh`: provision dependencies and service on Droplet
- `wifichat-server.service`: systemd unit for Java server
- `wifichat-backup.sh`: SQLite backup script
- `wifichat-backup.service`: oneshot backup unit
- `wifichat-backup.timer`: daily backup schedule

## Prerequisites

- Local: PowerShell, `ssh`, `scp`, JDK 17+, Maven
- Droplet: Ubuntu 24.04/22.04, SSH key login working

## 1) Build locally

From repo root:

```powershell
cd D:\Code\Java\test
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -SkipTests
```

Expected outputs:

- `out\wifi-chat-server.jar`
- `out\wifi-chat-client.jar`
- `out\wifi-chat-admin.jar`

## 2) Deploy server to Droplet

### Standard deploy (TCP 61000 + firewall)

```powershell
powershell -ExecutionPolicy Bypass -File deploy\digitalocean\deploy-to-droplet.ps1 `
  -ServerIp "167.71.201.89" `
  -User "root" `
  -KeyPath "$HOME\.ssh\DiOceanOpenSSH" `
  -SetupFirewall
```

### Redeploy quickly (skip local build)

```powershell
powershell -ExecutionPolicy Bypass -File deploy\digitalocean\deploy-to-droplet.ps1 `
  -ServerIp "167.71.201.89" `
  -User "root" `
  -KeyPath "$HOME\.ssh\DiOceanOpenSSH" `
  -SetupFirewall `
  -SkipBuild
```

### Optional: open UDP discovery port

```powershell
powershell -ExecutionPolicy Bypass -File deploy\digitalocean\deploy-to-droplet.ps1 `
  -ServerIp "167.71.201.89" `
  -User "root" `
  -KeyPath "$HOME\.ssh\DiOceanOpenSSH" `
  -SetupFirewall `
  -OpenUdpDiscovery `
  -UdpPort 50000
```

## 3) Verify server on Droplet (PuTTY/SSH)

```bash
systemctl status wifichat-server --no-pager
ss -lntp | grep 61000
journalctl -u wifichat-server -n 100 --no-pager
```

Expected:

- service = `active (running)`
- java process listening on `:61000`

## 4) Run app from local machine

### 4.1 User client (normal user portal)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 `
  -SkipBuild `
  -Mode hybrid `
  -ServerHost "167.71.201.89" `
  -ServerPort 61000 `
  -Profile A
```

### 4.2 Admin app (admin portal)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-admin.ps1 `
  -SkipBuild `
  -ServerHost "167.71.201.89" `
  -ServerPort 61000 `
  -Profile adminA
```

Default bootstrap admin account (new DB only):

- username: `admin`
- password: `admin123`

Change this password immediately after first login.

## 5) End-to-end test checklist

1. Login with 2 local clients (`Profile A`, `Profile B`) to same Droplet IP
2. Send messages in group chat and private chat
3. Open admin app, verify conversation list/history load
4. In SSH terminal run:

```bash
journalctl -u wifichat-server -f
```

Send message from client and confirm logs update in real time.

## 6) Package client for other computers (`dist`)

### 6.1 Build portable package preconfigured to your Droplet

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1 `
  -ServerHost "167.71.201.89" `
  -ServerPort 61000 `
  -Group 239.255.50.10 `
  -Port 50000 `
  -Room General
```

Outputs:

- `dist\WiFiChatClient-portable\`
- `dist\WiFiChatClient-portable.zip`
- optional installer `.exe` if `jpackage` + WiX are available

### 6.2 What to send

Send `dist\WiFiChatClient-portable.zip` to other users.  
They only need to unzip and run `start-client.bat`.

## 6.3 Quick Share Checklist (for another computer)

Use this when you send `WiFiChatClient-portable.zip` to someone else.

1. Download and unzip `WiFiChatClient-portable.zip`
2. Double-click `start-client.bat`
3. Register/login account on first run
4. Send a message in `General` room to confirm connection

Optional:

- To avoid profile collision on shared PCs, run:

```bat
start-client.bat MY_PROFILE_NAME
```

Troubleshooting:

- If app opens but cannot login/connect: verify server is running on Droplet (`systemctl status wifichat-server`)
- If Windows Defender/Firewall prompts, allow app/network access
- If server IP changes, regenerate package with `scripts\package-client.ps1` using new `-ServerHost`

## 7) Backup and operations

```bash
systemctl status wifichat-backup.timer --no-pager
systemctl start wifichat-backup.service
ls -lah /var/backups/wifichat
```

Server runtime paths:

- app jar: `/opt/wifichat/wifi-chat-server.jar`
- db: `/var/lib/wifichat/chat.db`
- logs: `/var/log/wifichat/server.log`, `/var/log/wifichat/server.err.log`

## Notes

- If apt mirror is inconsistent during deploy, rerun deploy; provisioning includes fallback for mirror metadata issues.
- UI user/admin on local machine looks the same whether backend is local or cloud; confirm by checking `-ServerHost` and server logs on Droplet.
