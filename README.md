# WiFi Chat Hybrid (Client + Server)

Ung dung Java chat da duoc nang cap tu UDP thu?n sang hybrid:

- `HYBRID` (mac dinh):
  - TCP la duong chat authoritative (auth + luu lich su + dong bo)
  - UDP giu discovery LAN/online hint + fallback
- `UDP_ONLY`: hanh vi legacy cu

## 1) Cau truc du an (Maven multi-module)

```text
.
|-- pom.xml                 (parent)
|-- shared/                (DTO/protocol/transport dung chung)
|-- server/                (TCP auth/history sync + SQLite)
|-- client/                (Swing GUI + UDP + TCP client + login)
|-- admin/                 (Swing Admin GUI + chat moderation tools)
`-- scripts/
    |-- build.ps1
    |-- run.ps1
    `-- run-server.ps1
```

## 2) Tinh nang da co

### Auth/account/session
- Dang ky tai khoan (`username + password + displayName`)
- Dang nhap
- Luu session local: `%USERPROFILE%\\.wifichat\\session-<profile>.bin` (mac dinh profile=`default`)
- Mo lai app se tu `resume_session` neu token con han

### TCP authoritative + SQLite
- TCP server mac dinh: `0.0.0.0:61000`
- Protocol: `length-prefixed JSON`
- SQLite schema:
  - `users`
  - `sessions`
  - `messages`
  - `conversation_members`
- Message duoc save server truoc khi broadcast lai (`message_saved` + `message_event`)

### Hybrid va fallback
- `--mode hybrid` (default): gui chat qua TCP, UDP de discovery
- `--mode udp-only`: giu hanh vi UDP legacy

### Lich su + dong bo
- Khi mo hoi thoai, client goi fetch history `last 150`
- Ho tro dedupe theo `messageId` de tranh trung khi reconnect
- Conversation key chuan hoa:
  - room: `room:<lowercase>`
  - pm: `pm:<smallerUserId>:<largerUserId>`

## 3) API TCP (v1)

Request:
- `register`
- `login`
- `resume_session`
- `send_message`
- `fetch_history`
- `subscribe_conversation`
- `heartbeat`
- `list_conversations`

Response/Event:
- `ok`
- `error`
- `message_saved`
- `history_batch`
- `message_event`
- `conversations`

Error codes:
- `AUTH_INVALID`
- `USERNAME_TAKEN`
- `SESSION_EXPIRED`
- `FORBIDDEN`
- `VALIDATION_ERROR`
- `INTERNAL_ERROR`

## 4) Lenh terminal can thiet

> Yeu cau: JDK 17+ va Maven 3.9+

### 4.1 Vao thu muc du an

```powershell
cd D:\Code\Java\test
```

### 4.2 Build app (client + server)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -SkipTests
```

Output:
- `out\wifi-chat-client.jar`
- `out\wifi-chat-server.jar`

### 4.3 Chay server TCP

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-server.ps1 -SkipBuild -Port 61000 -DbPath "D:\Code\Java\test\out\chat-server-1.db"
```

### 4.4 Chay client hybrid local (server tren cung may)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 127.0.0.1 -ServerPort 61000 -Group 239.255.50.10 -Port 50000 -Room General -Profile A
```

### 4.5 Chay client hybrid voi server cloud (DigitalOcean)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 167.71.201.89 -ServerPort 61000 -Group 239.255.50.10 -Port 50000 -Room General -Profile B
```

### 4.6 Chay admin app voi server cloud (DigitalOcean)

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-admin.ps1 -SkipBuild -ServerHost 167.71.201.89 -ServerPort 61000 -Profile adminA
```

### 4.7 Gia lap nhieu client tren cung 1 may

Mo nhieu terminal va chay moi terminal 1 lenh (doi `-Profile` va `-PrivatePort`):

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 127.0.0.1 -ServerPort 61000 -Profile A -PrivatePort 50011
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 127.0.0.1 -ServerPort 61000 -Profile B -PrivatePort 50012
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 127.0.0.1 -ServerPort 61000 -Profile C -PrivatePort 50013
```

### 4.8 Chay che do UDP-only fallback

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode udp-only -Name Alice -Group 239.255.50.10 -Port 50000 -Room General
```

### 4.9 Deploy server len DigitalOcean (tu may local)

```powershell
powershell -ExecutionPolicy Bypass -File deploy\digitalocean\deploy-to-droplet.ps1 -ServerIp 167.71.201.89 -User root -KeyPath "$HOME\.ssh\DiOceanOpenSSH" -SetupFirewall
```

Redeploy nhanh (bo qua build local):

```powershell
powershell -ExecutionPolicy Bypass -File deploy\digitalocean\deploy-to-droplet.ps1 -ServerIp 167.71.201.89 -User root -KeyPath "$HOME\.ssh\DiOceanOpenSSH" -SetupFirewall -SkipBuild
```

### 4.10 Dong goi ban gui khach (khong can cai Java)

Neu muon sach ban cu truoc khi dong goi:

```powershell
Remove-Item -Recurse -Force .\dist\*
```

Dong goi portable:

```powershell
powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1 -ServerHost 167.71.201.89 -ServerPort 61000 -Group 239.255.50.10 -Port 50000 -Room General
```

Output:
- Thu muc portable: `dist\WiFiChatClient-portable`
- File zip gui nguoi dung: `dist\WiFiChatClient-portable.zip`

### 4.11 Neu terminal khong nhan Maven (`mvn`)

```powershell
$env:Path="D:\Tools\apache-maven-3.9.9\bin;$env:Path"
mvn -v
```

### 4.12 Dung server/client

Nhan `Ctrl + C` o terminal dang chay.
## 5) Luu y trien khai LAN

- Tat ca may cung subnet/WiFi
- Mo firewall cho Java:
  - TCP port server (vd `61000`)
  - UDP multicast port (vd `50000`)
- Tat AP/client isolation neu router co bat

## 6) File quan trong

- Client entrypoint: `client/src/main/java/com/wifichat/Main.java`
- Hybrid node: `client/src/main/java/com/wifichat/network/ChatNode.java`
- Login/Register dialog: `client/src/main/java/com/wifichat/auth/LoginDialog.java`
- TCP client: `client/src/main/java/com/wifichat/tcp/TcpChatClient.java`
- Server entrypoint: `server/src/main/java/com/wifichat/server/ServerMain.java`
- Server TCP core: `server/src/main/java/com/wifichat/server/net/TcpChatServer.java`
- SQLite repo: `server/src/main/java/com/wifichat/server/db/ChatRepository.java`
- Shared protocol: `shared/src/main/java/com/wifichat/shared/protocol/*`

## 7) Han che hien tai

- Chua bat TLS (LAN-first)
- Da ho tro quyen ADMIN va app admin quan ly chat
- Chua co migration phuc tap cho schema (v1 auto-create)

## 8) Dong goi gui client (khong can cai Java)

Co script dong goi san:

~~~powershell
powershell -ExecutionPolicy Bypass -File scripts\package-client.ps1 -ServerHost 167.71.201.89 -ServerPort 61000 -Room General
~~~

Ket qua:
- Thu muc portable: `dist\WiFiChatClient-portable`
- File zip gui cho nguoi dung: `dist\WiFiChatClient-portable.zip`

Nguoi dung chi can giai nen zip va double-click `start-client.bat`.

Luu y ve installer `.exe`:
- Script se thu tao installer bang `jpackage`
- Neu may build chua cai WiX Toolset thi se skip installer va van co portable zip.



## 9) Admin app (quan ly chat)

### 9.1 Tai khoan admin mac dinh

Khi server khoi dong voi DB moi, he thong tu tao tai khoan admin:

- `username`: `admin`
- `password`: `admin123`

> Nen doi mat khau ngay sau khi trien khai thuc te.

### 9.2 Chay admin UI

```powershell
powershell -ExecutionPolicy Bypass -File scripts\run-admin.ps1 -SkipBuild -ServerHost 127.0.0.1 -ServerPort 61000 -Profile adminA
powershell -ExecutionPolicy Bypass -File scripts\run-admin.ps1 -SkipBuild -ServerHost 167.71.201.89 -ServerPort 61000 -Profile adminA
```

### 9.3 Chuc nang admin

- Xem toan bo conversations tren server
- Xem lich su chat cua bat ky conversation
- Xoa message cu the
- Xoa conversation
- Mute / Unmute user (chan/cho phep gui tin nhan)

