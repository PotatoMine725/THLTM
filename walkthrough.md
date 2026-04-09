# WiFi Chat UI Modernization — Walkthrough & Agent Rules

## Completed: Phase 1 — Shared Theme Foundation ✅

### Changes Made

**4 new files** created in `shared/src/main/java/com/wifichat/shared/ui/`:

| File | Purpose |
|------|---------|
| [AppTheme.java](file:///d:/Code/Java/test/shared/src/main/java/com/wifichat/shared/ui/AppTheme.java) | All color/font constants, cross-platform font fallback, `blend()` utility, admin-specific colors |
| [RoundedBorder.java](file:///d:/Code/Java/test/shared/src/main/java/com/wifichat/shared/ui/RoundedBorder.java) | Rounded-rect border, moved from client |
| [FlatLafBootstrap.java](file:///d:/Code/Java/test/shared/src/main/java/com/wifichat/shared/ui/FlatLafBootstrap.java) | Single `setup()` method replacing 15 duplicated lines in Main + AdminMain |
| [UIHelper.java](file:///d:/Code/Java/test/shared/src/main/java/com/wifichat/shared/ui/UIHelper.java) | `styleButton()`, `initials()`, `colorToHex()` — shared styling utilities |

**3 files modified**:

| File | Change |
|------|--------|
| [shared/pom.xml](file:///d:/Code/Java/test/shared/pom.xml) | Added FlatLaf 3.4.1 dependency |
| [Main.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/Main.java) | Replaced inline FlatLaf block → `FlatLafBootstrap.setup()` |
| [AdminMain.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/AdminMain.java) | Replaced inline FlatLaf block → `FlatLafBootstrap.setup()` |

**2 files converted to re-export aliases** (zero import changes in client code):

| File | Strategy |
|------|----------|
| [client AppTheme.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/AppTheme.java) | `extends com.wifichat.shared.ui.AppTheme` — inherits all static constants |
| [client RoundedBorder.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/RoundedBorder.java) | `extends com.wifichat.shared.ui.RoundedBorder` — delegates constructor |

**1 file cleaned up**:

| File | Change |
|------|--------|
| [MainFrame.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/MainFrame.java) | Removed 5 unused imports, fixed `\r\r\n` → `\r\n` line endings |

### Build Status
✅ `mvn compile` passes for shared, client, server, admin. All 3 JARs produced.

---

## Remaining Phases — Agent Implementation Rules

> [!IMPORTANT]
> The following rules apply to **any agent** implementing Phases 2-6. These are non-negotiable constraints to minimize blast radius and preserve correctness.

### Architecture Reference

```
shared/src/main/java/com/wifichat/shared/ui/
├── AppTheme.java          ← THE source of truth for all colors/fonts
├── RoundedBorder.java     ← Shared rounded border
├── FlatLafBootstrap.java  ← Call once in main()
└── UIHelper.java          ← styleButton(), initials(), colorToHex()

client/src/main/java/com/wifichat/ui/
├── AppTheme.java          ← Re-export alias (extends shared)
├── RoundedBorder.java     ← Re-export alias (extends shared)
├── MainFrame.java         ← 1483 lines, TO BE DECOMPOSED in Phase 3
└── MessageCellRenderer.java

admin/src/main/java/com/wifichat/admin/ui/
└── AdminFrame.java        ← TO BE MODERNIZED in Phase 2
```

---

### Rule 1: One Phase at a Time
Complete each phase fully, build-verify, before starting the next. Phases depend on each other:
- Phase 2 depends on Phase 1 (shared theme)
- Phase 3 is independent of Phase 2
- Phase 4 depends on Phase 3 (extracted renderer)
- Phase 5 is independent
- Phase 6 is last

### Rule 2: No Behavior Changes
Every phase is styling or refactoring. No new features, no protocol changes, no data model changes.

### Rule 3: Preserve All Public APIs
These constructor signatures must NOT change:
```java
MainFrame(AppConfig config, ChatNode node)
MainFrame(AppConfig config, ChatNode node, Runnable onLogout)
AdminFrame(AdminTcpClient tcpClient, AuthSession session, Runnable onLogout)
```

### Rule 4: Use Shared Theme — Never Raw Colors
```java
// ✅ CORRECT
panel.setBackground(AppTheme.SIDEBAR_BG);
UIHelper.styleButton(btn, AppTheme.PRIMARY_BUTTON, ...);

// ❌ WRONG
panel.setBackground(Color.decode("#2B2D31"));
panel.setBackground(new Color(43, 45, 49));
```

### Rule 5: Use Shared Fonts — Never Raw Font Constructors
```java
// ✅ CORRECT
label.setFont(AppTheme.heading(16));
label.setFont(AppTheme.body(Font.BOLD, 14));

// ❌ WRONG
label.setFont(new Font("Segoe UI", Font.BOLD, 14));
```

### Rule 6: Use UIHelper.styleButton() — Never Manual Button Styling
```java
// ✅ CORRECT
JButton btn = new JButton("Delete");
UIHelper.styleButton(btn, AppTheme.DANGER_BUTTON, AppTheme.DANGER_BUTTON.brighter(), AppTheme.TEXT_PRIMARY, 10, 5, 9);

// ❌ WRONG — manual hover/pressed listeners on buttons
```

### Rule 7: Decomposition Rules (Phase 3)
- Copy exact method bodies into new classes; do NOT refactor logic during extraction
- New panel classes are `JPanel` subclasses
- Pass dependencies through constructors (models, callbacks)
- Inner renderers become package-private top-level classes

### Rule 8: Build After Every File Change
```powershell
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -SkipTests
```

### Rule 9: Server Module is Read-Only
No changes to any file under `server/`.

### Rule 10: Shared Protocol/DTO is Read-Only
No changes to `shared/src/main/java/com/wifichat/shared/protocol/` or `shared/src/main/java/com/wifichat/shared/dto/`.

### Rule 11: Python Scripts — Keep But Don't Use
The files `fix.py`, `style_messenger.py`, `style_messenger2.py`, `fix_admin.py` are historical artifacts. Their patches are already applied. Do NOT run them or delete them.

### Rule 12: Admin Colors Reference
When styling admin components, use these constants from `AppTheme`:
- `ADMIN_ACCENT` — Red accent for admin-specific highlights
- `ADMIN_HEADER_BG` — Darker header background
- `MUTED_USER_BG` — Subtle red tint for muted user rows
- All other colors shared with client

---

## Phase 2 Spec: Admin UI Modernization

### Target: [AdminFrame.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/ui/AdminFrame.java)

**Current state**: Generic Swing look. Uses `Color.decode()` inline, `new Font()` inline, default `DefaultListCellRenderer` with just text.

**Target state**: Discord-inspired dark theme matching client. Custom renderers for all 3 lists.

#### Changes:
1. Replace ALL `Color.decode("#...")` → `AppTheme.*` constants
2. Replace ALL `new Font("Segoe UI", ...)` → `AppTheme.body(...)` / `AppTheme.heading(...)`
3. Style ALL buttons using `UIHelper.styleButton()`
4. Style section headers: `AppTheme.SIDEBAR_HEADER_TEXT`, `AppTheme.body(Font.BOLD, 12)`
5. Custom `ConversationRenderer`:
   - Hash `#` prefix for rooms, `@` prefix for PMs
   - `AppTheme.SIDEBAR_ITEM_BG` / `SIDEBAR_ITEM_ACTIVE_BG` backgrounds
   - `AppTheme.SIDEBAR_CHANNEL_HASH` for hash color
6. Custom `UserRenderer`:
   - Avatar initials circle (use `UIHelper.initials()`)
   - Role badge (ADMIN in accent color, USER muted)
   - Muted indicator: `[MUTED]` prefix + `AppTheme.MUTED_USER_BG` row background
7. Custom `MessageRenderer`:
   - `[timestamp] sender: content` format with styled colors
   - `AppTheme.TEXT_MUTED` for timestamp, `AppTheme.TEXT_SECONDARY` for sender
8. Sidebar dividers between sections
9. Profile card at top showing admin name + role

---

## Phase 3 Spec: MainFrame Decomposition

### Strategy: Extract → Delegate

Create these files in `client/src/main/java/com/wifichat/ui/`:

| New File | Extracted From | Methods |
|----------|---------------|---------|
| `sidebar/SidebarPanel.java` | MainFrame | `buildSidebar()`, `buildProfileCard()`, `buildChannelSection()`, `buildDirectMessageSection()`, `buildOnlinePeersSection()`, `buildSidebarDivider()`, `wrapSidebarList()`, `createSectionHeader()`, `createUnreadHeaderBadge()`, `promptJoinRoom()`, `handleRoomSelection()`, `openSelectedPrivateChat()` |
| `sidebar/RoomListRenderer.java` | MainFrame | Inner class `RoomListRenderer` |
| `sidebar/DirectMessageListRenderer.java` | MainFrame | Inner class `DirectMessageListRenderer` |
| `sidebar/PeerListRenderer.java` | MainFrame | Inner class `PeerListRenderer` |
| `chat/MessagePanel.java` | MainFrame | `buildMessageCard()`, `buildConversationHeader()` |
| `chat/ComposerPanel.java` | MainFrame | `buildComposerPanel()`, reply context management |

MainFrame keeps: constructor glue, `ChatNodeListener` callbacks, conversation state maps, `appendMessage()`, `scrollToBottom()`, `loadHistory*()`, `sendCurrentMessage()`.

---

## Phase 4-6 Specs

### Phase 4: Message Renderer
- Replace `BubblePanel` custom paint → `putClientProperty("JComponent.roundRect", true)`
- Consistent HTML: `<html><body style='width:Npx'>` (not `max-width`)

### Phase 5: Login Dialogs
- Both dialogs: dark background, styled inputs, styled buttons, branding header
- Client: `AppTheme.PRIMARY_BUTTON` login button, `AppTheme.ITEM_BG` text fields
- Admin: add "ADMIN" badge, `AppTheme.ADMIN_ACCENT` accent

### Phase 6: Cleanup
- Remove trailing blank lines (MainFrame lines 1450+)
- Final lint pass
- Final build verification
