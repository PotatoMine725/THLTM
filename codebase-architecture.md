# WiFi Chat Hybrid - Tai lieu cau truc codebase

Tai lieu nay mo ta chi tiet cau truc du an, cach to chuc thu muc, chuc nang tung module, va luong hoat dong cua he thong.

Luu y:
- Co y bo qua noi dung trong `legacy_src_backup/`.
- Cac thu muc `target/` la output build, khong phai source code chinh.
- Tai lieu da luoc bo mot so chi tiet nhay cam lien quan den van hanh va bao mat.

## 1) Tong quan kien truc

Du an la Maven multi-module gom 4 module chinh:
- `shared`: Dinh nghia du lieu/protocol/UI dung chung.
- `server`: TCP server authoritative + luu tru SQLite + auth/session + moderation API.
- `client`: Ung dung chat nguoi dung (Swing), ho tro `HYBRID` (TCP + UDP) va `UDP_ONLY`.
- `admin`: Ung dung quan tri (Swing), thao tac moderation tren server.

Kieu trien khai chinh:
- Che do mac dinh: `HYBRID`
- TCP dung cho auth, luu lich su, dong bo message, quy dinh quyen.
- UDP giu vai tro discovery LAN/peer hint (va fallback cho `UDP_ONLY`).

## 2) Cau truc thu muc cap cao

```text
.
|-- pom.xml                          # Parent Maven, khai bao modules
|-- README.md                        # Huong dan su dung nhanh
|-- codebase-architecture.md         # Tai lieu nay
|-- scripts/                         # Script build/run/package local
|-- deploy/                          # Script va tai lieu deploy server
|   `-- digitalocean/
|-- shared/                          # Code dung chung giua cac module
|-- server/                          # Backend TCP + SQLite
|-- client/                          # Ung dung chat cho user
`-- admin/                           # Ung dung admin quan tri chat
```

## 3) Parent Maven va dependency graph

### 3.1 Parent POM
File: `pom.xml`
- Packaging: `pom`
- Modules:
  - `shared`
  - `server`
  - `client`
  - `admin`
- Java version: 17
- Property SQLite JDBC version: `3.47.0.0`

### 3.2 Quan he phu thuoc giua cac module
- `shared`: doc lap (chua DTO/protocol/util/ui common).
- `server` -> phu thuoc `shared` + `sqlite-jdbc`.
- `client` -> phu thuoc `shared` + `flatlaf`.
- `admin` -> phu thuoc `shared` + `flatlaf`.

## 4) Chi tiet tung module

## 4.1 Module `shared/`
Muc tieu: dung chung contract giua server, client, admin.

Cau truc chinh:
- `shared/src/main/java/com/wifichat/shared/dto/`
  - Cac request/response payload cho TCP API:
    - auth: `LoginRequest`, `RegisterRequest`, `ResumeSessionRequest`, `AuthResponse`
    - message/history: `SendMessageRequest`, `MessageRecord`, `FetchHistoryRequest`, `HistoryBatchResponse`
    - conversation: `ListConversationsRequest`, `ConversationsResponse`, `SubscribeConversationRequest`
    - admin: `AdminListUsersRequest`, `AdminUsersResponse`, `AdminDeleteMessageRequest`, ...
- `shared/src/main/java/com/wifichat/shared/protocol/`
  - `PacketTypes`: danh sach loai goi tin logic (`login`, `send_message`, `message_event`, ...)
  - `ErrorCodes`: ma loi thong nhat (`AUTH_INVALID`, `FORBIDDEN`, `SESSION_EXPIRED`, ...)
  - `WireEnvelope`: envelope bao goi packet
  - `WireCodec`: encode/decode frame theo `length-prefixed + object serialization`
- `shared/src/main/java/com/wifichat/shared/util/`
  - `ConversationKeys`: quy tac key cho room/PM
- `shared/src/main/java/com/wifichat/shared/ui/`
  - Utility UI dung chung (FlatLaf bootstrap/theme)
- `shared/src/main/java/com/wifichat/shared/TransportMode.java`
  - Enum transport mode (`HYBRID`, `UDP_ONLY`)

Gia tri module `shared`:
- Dinh nghia giao tiep on dinh giua client/server/admin.
- Tranh duplicated model/protocol.
- De nang cap protocol co kiem soat.

## 4.2 Module `server/`
Muc tieu: server authoritative cho auth/session/message/history/moderation.

Cau truc package:
- `server/src/main/java/com/wifichat/server/ServerMain.java`
  - Entrypoint server.
  - Parse arg `--port`, `--db`.
  - Khoi tao DB path mac dinh: `~/.wifichat-server/chat.db` neu khong truyen `--db`.
  - Khoi tao `ChatRepository`, `init()` schema.
  - Chay `TcpChatServer`.
- `server/src/main/java/com/wifichat/server/net/`
  - `TcpChatServer`: TCP listener + client handler + request dispatcher.
  - Quan ly subscription theo `conversationKey` de broadcast real-time.
- `server/src/main/java/com/wifichat/server/db/`
  - `ChatRepository`: toan bo truy cap SQLite (users/sessions/messages/conversation_members/muted_users).
- `server/src/main/java/com/wifichat/server/auth/`
  - `PasswordService`: hash/verify password voi salt.
- `server/src/main/java/com/wifichat/server/model/`
  - Model noi bo: `UserAccount`, `AuthContext`, `SessionInfo`, ...

Chuc nang chinh:
- Dang ky/dang nhap/resume session.
- Xac thuc theo token session.
- Luu message vao SQLite truoc khi phat su kien.
- Lay lich su theo conversation.
- List conversation theo user hoac toan bo (admin).
- Moderation API:
  - list users
  - mute/unmute user
  - delete message
  - delete conversation

## 4.3 Module `client/`
Muc tieu: app chat desktop cho user thong thuong.

Cau truc package:
- `client/src/main/java/com/wifichat/Main.java`
  - Entrypoint client.
  - Setup LAF, parse config, re nhanh theo mode:
    - `HYBRID`: TCP auth + TCP message + UDP discovery
    - `UDP_ONLY`: LAN UDP legacy mode
- `client/src/main/java/com/wifichat/config/`
  - `AppConfig`: map CLI args (`--mode`, `--server-host`, `--room`, `--profile`, ...)
- `client/src/main/java/com/wifichat/auth/`
  - `LoginDialog`, `SessionStore`, `AuthSession`
  - Quan ly login/register va cache session local theo profile
- `client/src/main/java/com/wifichat/network/`
  - `ChatNode`: runtime networking core phia client
  - Quan ly sockets UDP multicast/private, peers, heartbeat, prune peers
  - Tich hop `TcpChatClient` khi mode HYBRID
- `client/src/main/java/com/wifichat/tcp/`
  - `TcpChatClient`: request/response TCP voi pending map theo requestId
  - Ho tro event callback `message_event` theo listener
- `client/src/main/java/com/wifichat/ui/`
  - `MainFrame`, renderer, cac thanh phan giao dien chat
- `client/src/main/java/com/wifichat/model/`
  - Chat message/network packet/peer model
- `client/src/main/java/com/wifichat/util/`
  - Utility phu tro

Chuc nang chinh:
- Login, register, resume session.
- Chat room va private message.
- Dong bo lich su chat.
- List conversation da tham gia.
- Re-login khi logout/session het han.

## 4.4 Module `admin/`
Muc tieu: app desktop cho quan tri vien.

Cau truc package:
- `admin/src/main/java/com/wifichat/admin/AdminMain.java`
  - Entrypoint admin app.
  - Ket noi TCP server, resume/login, khoi tao `AdminFrame`.
- `admin/src/main/java/com/wifichat/admin/tcp/`
  - `AdminTcpClient`: wrapper goi cac admin API qua TCP.
- `admin/src/main/java/com/wifichat/admin/auth/`
  - `LoginDialog`, `SessionStore`, `AuthSession` cho admin.
- `admin/src/main/java/com/wifichat/admin/config/`
  - `AdminConfig` parse server host/port/profile.
- `admin/src/main/java/com/wifichat/admin/ui/`
  - `AdminFrame`: danh sach conversation, message history, users, thao tac moderation.

Chuc nang chinh:
- Dang nhap bang role ADMIN.
- Xem tat ca conversation.
- Xem lich su theo conversation.
- Xoa message / xoa conversation.
- Mute/unmute user.

## 4.5 `scripts/` va `deploy/`

### `scripts/`
- `build.ps1`
  - Build Maven toan bo modules, dong goi jar vao `out/`:
    - `out/wifi-chat-client.jar`
    - `out/wifi-chat-server.jar`
    - `out/wifi-chat-admin.jar`
  - Co fallback `javac` khi khong tim thay Maven.
- `run-server.ps1`
  - Chay server jar, verify co `sqlite-jdbc` trong jar.
- `run.ps1`
  - Chay client jar voi tham so mode/network/profile.
- `run-admin.ps1`
  - Chay admin jar.
- `package-client.ps1`
  - Dong goi ban client portable.

### `deploy/digitalocean/`
- Script provision/deploy service cho Linux droplet.
- Chua service unit (`wifichat-server.service`) va backup timer/service.

## 5) Luong hoat dong runtime

## 5.1 Luong khoi dong server
1. Chay `ServerMain`.
2. Parse `--port`, `--db`.
3. Khoi tao `ChatRepository` voi JDBC SQLite URL.
4. `init()` tao/upgrade bang neu chua ton tai.
5. Khoi tao `TcpChatServer`.
6. Accept ket noi client lien tuc va tao `ClientHandler` theo moi socket.

## 5.2 Luong auth client HYBRID
1. `Main` parse config, vao `runHybrid()`.
2. Tao `TcpChatClient` va `connect()` den `server-host:server-port`.
3. Thu `resume_session` tu `SessionStore`.
4. Neu resume that bai -> hien `LoginDialog` cho login/register.
5. Nhan `AuthResponse` va luu lai session local.
6. Tao `ChatNode` voi `userId`, `displayName`, `sessionToken`, `tcpClient`.
7. Khoi tao `MainFrame`, bind listener, start node.

## 5.3 Luong xu ly message (group/private) trong HYBRID
1. User gui tin nhan tu UI -> `ChatNode.sendGroupMessage` hoac `sendPrivateMessage`.
2. `ChatNode` map sang `MessageRecord`, gan `scope`, `conversationKey`, thong tin target neu PM.
3. `TcpChatClient.sendMessage()` gui request `send_message`.
4. Server `handleSendMessage()`:
   - validate session
   - check muted
   - normalize message fields
   - tinh `conversationKey` chuan hoa
   - save DB qua `ChatRepository.saveMessage()`
   - tra `message_saved` cho caller
   - `broadcast(message_event)` cho cac client da subscribe conversation
5. Client nhan `message_event` o read loop -> callback `TcpEventListener` -> `ChatNode.handleTcpMessage()` -> UI update.

## 5.4 Luong history va dong bo conversation
1. UI mo conversation.
2. Client goi `subscribe_conversation` (neu chua subscribe).
3. Client goi `fetch_history(sessionToken, conversationKey, limit)`.
4. Server check quyen truy cap conversation (member hoac admin).
5. Server tra `history_batch`.
6. Client render lich su theo thu tu timestamp tang dan.

## 5.5 Luong admin moderation
1. Admin app login/resume qua `AdminTcpClient`.
2. Server check role ADMIN.
3. Admin frame goi cac API:
   - list conversations (toan bo)
   - list users
   - delete message
   - delete conversation
   - set user muted/unmuted
4. Server thuc hien thao tac trong `ChatRepository`.

## 5.6 Luong UDP_ONLY (legacy compatibility)
1. `Main` re nhanh vao `runUdpOnly()`.
2. `ChatNode` chay UDP multicast/private, khong can TCP session.
3. Message/announcement trao doi theo packet model LAN.
4. Khong co server-authoritative history va moderation day du nhu HYBRID.

## 6) Data model va persistence (SQLite)

Bang chinh trong server:
- `users`
  - user account + role + thong tin xac thuc
- `sessions`
  - session token, TTL, last_seen
- `messages`
  - message da duoc save theo conversation key
- `conversation_members`
  - thanh vien PM/conversation de check quyen
- `muted_users`
  - danh sach user bi mute boi admin

Diem can luu y:
- Session TTL mac dinh 7 ngay.
- Co co che touch `last_seen_at` theo interval.
- Room key va PM key duoc normalize qua `ConversationKeys`.

## 7) Giao tiep TCP (logical API)

Request types tieu bieu:
- auth: `register`, `login`, `resume_session`
- message: `send_message`, `fetch_history`, `subscribe_conversation`
- misc: `heartbeat`, `list_conversations`
- admin: `admin_list_users`, `admin_delete_message`, `admin_set_user_muted`, ...

Response/event types tieu bieu:
- `ok`, `error`
- `message_saved`
- `history_batch`
- `message_event`
- `conversations`
- `admin_users`

## 8) Quy uoc to chuc thu muc trong codebase

- Moi module tuan theo Maven standard layout:
  - `src/main/java/...`
- Moi module co package root ro rang:
  - Client: `com.wifichat`
  - Server: `com.wifichat.server`
  - Admin: `com.wifichat.admin`
  - Shared: `com.wifichat.shared`
- Logic networking va UI duoc tach package rieng.
- Contract du lieu/protocol de trong `shared` de tranh circular dependency.
- Script van hanh dat trong `scripts/`, script deploy dat trong `deploy/`.

## 9) Entry points quan trong

- Client app: `client/src/main/java/com/wifichat/Main.java`
- Server app: `server/src/main/java/com/wifichat/server/ServerMain.java`
- Admin app: `admin/src/main/java/com/wifichat/admin/AdminMain.java`

Core runtime classes:
- Client networking: `client/src/main/java/com/wifichat/network/ChatNode.java`
- Client TCP layer: `client/src/main/java/com/wifichat/tcp/TcpChatClient.java`
- Server TCP core: `server/src/main/java/com/wifichat/server/net/TcpChatServer.java`
- Server persistence: `server/src/main/java/com/wifichat/server/db/ChatRepository.java`
- Shared protocol: `shared/src/main/java/com/wifichat/shared/protocol/`

## 10) Ghi chu pham vi

Tai lieu nay tap trung vao codebase hien tai dang duoc su dung de build/chay app.
Khong dua thong tin van hanh nhay cam (tai khoan, secret, IP/host production, key truy cap).
Khong bao gom va khong phan tich noi dung trong `legacy_src_backup/`.
