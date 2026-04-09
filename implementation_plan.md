# WiFi Chat UI Modernization Plan

## Code Review Summary

### 📊 Inventory

| Module | File | Lines | Role |
|--------|------|-------|------|
| **Client** | [MainFrame.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/MainFrame.java) | 1488 | Main chat window (sidebar, messages, composer) |
| **Client** | [MessageCellRenderer.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/MessageCellRenderer.java) | 298 | Messenger-style bubble renderer |
| **Client** | [AppTheme.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/AppTheme.java) | 82 | Centralized color/font constants |
| **Client** | [RoundedBorder.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/RoundedBorder.java) | 47 | Custom rounded-rect border |
| **Client** | [LoginDialog.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/auth/LoginDialog.java) | 131 | Login/Register tabbed dialog |
| **Client** | [Main.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/Main.java) | 157 | Entry point, FlatLaf setup |
| **Admin** | [AdminFrame.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/ui/AdminFrame.java) | 382 | Admin moderation panel |
| **Admin** | [LoginDialog.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/auth/LoginDialog.java) | 89 | Admin login dialog |
| **Admin** | [AdminMain.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/AdminMain.java) | 125 | Entry point, FlatLaf setup |

---

### 🔍 Issues Found

#### Architecture Issues

1. **Monolithic MainFrame (1488 lines)** — The entire client UI lives in one file: sidebar, message panel, composer, 3 inner renderers, all event handlers, conversation logic, unread tracking, and utility methods. This makes any targeted change risky and hard to test.

2. **Duplicated FlatLaf bootstrap** — Identical 15-line FlatLaf config blocks in both [Main.java:21-36](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/Main.java#L21-L36) and [AdminMain.java:20-37](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/AdminMain.java#L20-L37). Any change must be made in two places.

3. **No shared theme between client and admin** — Client uses `AppTheme.java` with a rich color system. Admin hardcodes raw `Color.decode("#313338")` inline, resulting in inconsistent look and fragile maintenance.

4. **3 inner classes as renderers** — `RoomListRenderer`, `DirectMessageListRenderer`, and `PeerListRenderer` are private inner classes inside MainFrame, making them impossible to reuse or test independently.

#### Theming & Styling Issues

5. **Admin UI is unstyled** — AdminFrame uses raw default Swing styling. No custom colors on buttons, no custom fonts on section headers, no badge system on users/conversations. The admin looks like a 2005 Swing app next to the Discord-inspired client.

6. **Hard-coded fonts** — `"Bahnschrift"` and `"Segoe UI"` are Windows-only fonts with no fallback chain. These will render as generic serif on Linux/macOS.

7. **Python fix scripts as technical debt** — `fix.py`, `fix_admin.py`, `style_messenger.py`, `style_messenger2.py` are ad-hoc Python scripts that patch Java source files via string replacement. This indicates previous changes were applied outside normal version control workflow and should be integrated properly.

#### Rendering Issues

8. **BubblePanel custom painting** — `MessageCellRenderer.BubblePanel` overrides `paintComponent` with manual `fillRoundRect`. FlatLaf already supports `JComponent.roundRect` client property, making this custom painting unnecessary and a source of visual seams.

9. **HTML content rendering in JLabel** — Message content uses `<html><div style='max-width:...'` inside JLabels. This is fragile, produces inconsistent sizing in list cells, and doesn't support word-wrap correctly in all edge cases.

#### UX Issues

10. **Login dialogs are bare-bones** — Both client and admin LoginDialogs use raw `GridLayout` with no theming, no branding, no visual hierarchy. First impression of the app is a generic 1990s dialog box.

11. **No hover effects on sidebar items** — Room list, DM list, and peer list have static backgrounds. Modern chat UIs (Discord, Slack) show subtle hover feedback.

12. **Typing indicator is local-only** — `updateTypingIndicator()` only shows the current user's own typing state, which is pointless as self-feedback.

---

## User Review Required

> [!IMPORTANT]
> **Shared theme module**: The plan proposes moving `AppTheme.java` and `RoundedBorder.java` to the `shared` module so both client and admin can use them. This changes the Maven dependency graph slightly — both modules already depend on `shared`, so this is additive only. Please confirm this approach.

> [!IMPORTANT]
> **MainFrame decomposition**: Phase 3 splits MainFrame into 5-6 smaller files. This is the highest blast-radius change. It changes no behavior, only code organization. Please confirm you want this level of refactoring, or if you'd prefer to keep MainFrame as-is and only change styling.

> [!WARNING]
> **Python fix scripts**: The plan recommends deleting `fix.py`, `fix_admin.py`, `style_messenger.py`, `style_messenger2.py` since their changes are already applied to the source. Confirm these are no longer needed.

---

## Proposed Changes

Changes are organized into 6 phases, ordered by dependency. Each phase is independently buildable and testable.

---

### Phase 1: Shared Theme Foundation

**Goal**: Eliminate duplicated theming and give admin access to the same design system as the client.

#### [MOVE] AppTheme.java → shared module
- Move from `client/src/main/java/com/wifichat/ui/AppTheme.java`
- To `shared/src/main/java/com/wifichat/shared/ui/AppTheme.java`
- Update package declaration and all import references in client

#### [MOVE] RoundedBorder.java → shared module
- Move from `client/src/main/java/com/wifichat/ui/RoundedBorder.java`
- To `shared/src/main/java/com/wifichat/shared/ui/RoundedBorder.java`
- Update package declaration and all import references

#### [NEW] shared/.../ui/FlatLafBootstrap.java
- Extract the 15-line FlatLaf config into a single static method `FlatLafBootstrap.setup()`
- Both `Main.java` and `AdminMain.java` call this one method

#### [MODIFY] [Main.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/Main.java)
- Replace inline FlatLaf config with `FlatLafBootstrap.setup()`
- Update `AppTheme` imports to new shared package

#### [MODIFY] [AdminMain.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/AdminMain.java)
- Replace inline FlatLaf config with `FlatLafBootstrap.setup()`

#### [MODIFY] AppTheme.java (in new location)
- Add fallback font chain: `"Segoe UI", "SF Pro Text", ".SF NS Text", "Helvetica Neue", sans-serif`
- Add heading fallback: `"Bahnschrift", "SF Pro Display", "Helvetica Neue", sans-serif`
- Add admin-specific constants: `ADMIN_ACCENT`, `ADMIN_HEADER_BG`, `MUTED_USER_BG`

---

### Phase 2: Admin UI Modernization

**Goal**: Bring AdminFrame to visual parity with the client's Discord-inspired design.

#### [MODIFY] [AdminFrame.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/ui/AdminFrame.java)

Changes:
- Replace all `Color.decode("#...")` with `AppTheme.*` constants
- Replace `new Font("Segoe UI", ...)` with `AppTheme.body(...)` / `AppTheme.heading(...)`
- Add styled buttons using the same `styleButton()` pattern from MainFrame (extract as shared utility)
- Add custom `UserRenderer` with avatar initials, role badge, mute indicator styled like client's PeerListRenderer
- Add custom `ConversationRenderer` with hash/@ prefixes and styled backgrounds
- Add custom `MessageRenderer` with bubble-style rendering (reuse `MessageCellRenderer` pattern)
- Add sidebar dividers and section headers matching client style
- Add proper conversation header with title and action buttons

#### [MODIFY] [admin LoginDialog.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/auth/LoginDialog.java)

Changes:
- Apply dark theme background (`AppTheme.WINDOW_BG`)
- Style input fields with `AppTheme.ITEM_BG` and `AppTheme.TEXT_PRIMARY`
- Style login button with `AppTheme.PRIMARY_BUTTON`
- Add admin badge/shield icon in header
- Increase dialog size for better proportions

---

### Phase 3: Client MainFrame Decomposition

**Goal**: Break the 1488-line monolith into focused components without changing any behavior.

#### [NEW] client/.../ui/sidebar/SidebarPanel.java
- Extract `buildSidebar()`, `buildProfileCard()`, `buildChannelSection()`, `buildDirectMessageSection()`, `buildOnlinePeersSection()`, `buildSidebarDivider()`, `wrapSidebarList()`, `createSectionHeader()`

#### [NEW] client/.../ui/sidebar/RoomListRenderer.java
- Extract `RoomListRenderer` inner class

#### [NEW] client/.../ui/sidebar/DirectMessageListRenderer.java
- Extract `DirectMessageListRenderer` inner class

#### [NEW] client/.../ui/sidebar/PeerListRenderer.java
- Extract `PeerListRenderer` inner class

#### [NEW] client/.../ui/chat/MessagePanel.java
- Extract `buildMessageCard()`, `buildConversationHeader()`

#### [NEW] client/.../ui/chat/ComposerPanel.java
- Extract `buildComposerPanel()`, reply context management

#### [MODIFY] [MainFrame.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/MainFrame.java)
- Replace inline UI building with composition of new panel classes
- Keep all `ChatNodeListener` callbacks and conversation state management
- Keep `DirectMessageEntry` as a shared model class
- Reduce from ~1488 to ~400-500 lines

---

### Phase 4: Message Renderer Improvements

**Goal**: Fix rendering quality issues in the bubble-style message display.

#### [MODIFY] [MessageCellRenderer.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/ui/MessageCellRenderer.java)

Changes:
- Remove custom `BubblePanel` inner class
- Replace with standard `JPanel` + `putClientProperty("JComponent.roundRect", true)` (FlatLaf native)
- Switch from `<html><div style='max-width:...'>` to `<html><body style='width:...'>` for more consistent wrapping
- Add hover highlight on message rows (subtle background change on mouse-over)
- Clean up paint logic for antialiased avatar rendering

---

### Phase 5: Login Dialog Modernization

**Goal**: Make the first-impression screens look professional and branded.

#### [MODIFY] [client LoginDialog.java](file:///d:/Code/Java/test/client/src/main/java/com/wifichat/auth/LoginDialog.java)

Changes:
- Apply dark theme: `AppTheme.WINDOW_BG` background, `AppTheme.TEXT_PRIMARY` foreground
- Style `JTabbedPane` with FlatLaf properties for tab styling
- Add app title/logo header area above the tabs
- Style text fields: `AppTheme.ITEM_BG` background, rounded corners
- Style buttons: primary button styling with hover effects
- Add subtle border and padding improvements
- Increase dialog size to `480 × 340`

#### [MODIFY] [admin LoginDialog.java](file:///d:/Code/Java/test/admin/src/main/java/com/wifichat/admin/auth/LoginDialog.java)
- Same dark theme treatment as client LoginDialog
- Add "ADMIN" role indicator in the UI
- Styled to match admin color accent

---

### Phase 6: Cleanup

**Goal**: Remove technical debt.

#### [DELETE] client/fix.py
#### [DELETE] client/style_messenger.py
#### [DELETE] client/style_messenger2.py
#### [DELETE] admin/fix_admin.py

These Python patch scripts have already been applied. They are confusing to future developers and serve no purpose.

#### [MODIFY] MainFrame.java
- Remove trailing blank lines (lines 1450-1488 are empty)
- Remove dead `GradientPaint` import if unused after refactor
- Remove unused `FocusAdapter`/`FocusEvent` imports

---

## Agent Implementation Rules

> [!IMPORTANT]
> Agents implementing this plan MUST follow these rules exactly.

### General Rules

1. **One phase at a time** — Complete and build-test each phase before starting the next. Never mix phases.
2. **No behavior changes** — Every phase is a pure refactor or styling change. The app must function identically before and after each phase.
3. **Preserve all existing public API** — `MainFrame(AppConfig, ChatNode)`, `MainFrame(AppConfig, ChatNode, Runnable)`, `AdminFrame(AdminTcpClient, AuthSession, Runnable)` constructors must not change signature.
4. **Build verification after each phase** — Run `mvn compile -pl shared,client,admin` and fix any errors before proceeding.

### Theming Rules

5. **Never use raw `Color` or `new Color(r,g,b)` in UI code** — All colors must come from `AppTheme.*` constants.
6. **Never use `new Font(...)` in UI code** — All fonts must come from `AppTheme.heading()` or `AppTheme.body()`.
7. **Font names must include fallback** — Use CSS-style fallback in `Font` constructor or `UIManager.put("defaultFont", ...)` with FlatLaf.
8. **Admin and Client must be visually consistent** — Same dark backgrounds, same border colors, same button styling, same font hierarchy.

### Decomposition Rules

9. **Extract, don't rewrite** — When splitting MainFrame, copy the exact method bodies into new classes. Do not "improve" them during extraction.
10. **New classes extend JPanel** — Extracted sidebar/message/composer panels are `JPanel` subclasses with their own constructors.
11. **Pass dependencies through constructors** — New panel classes receive their data models and callbacks via constructor parameters, not by reaching into MainFrame.
12. **Inner renderers become package-private top-level classes** — Move each renderer to its own file in the same package.

### Styling Rules

13. **Use FlatLaf client properties where possible** — Prefer `putClientProperty("JComponent.roundRect", true)` over custom `paintComponent`.
14. **Hover effects use MouseAdapter** — Consistent pattern: `mouseEntered` → lighter bg, `mouseExited` → restore original.
15. **Buttons use the `styleButton()` utility** — Extract MainFrame's `styleButton` to a shared class (`UIHelper` or into `AppTheme`) so both client and admin can use it.
16. **Sidebar list items have 8px vertical padding** — Consistent `EmptyBorder(8, 8, 8, 8)` on all sidebar renderers.

### Safety Rules

17. **Keep backup of MainFrame before Phase 3** — Copy `MainFrame.java` to `MainFrame.java.bak` in the same directory before decomposition.
18. **Test after every file move** — After moving AppTheme/RoundedBorder to shared, immediately run compile to catch import issues.
19. **Do NOT modify server module** — No changes to any file under `server/`.
20. **Do NOT modify shared protocol/DTO** — No changes to `shared/src/main/java/com/wifichat/shared/protocol/` or `shared/src/main/java/com/wifichat/shared/dto/`.

---

## Open Questions

> [!IMPORTANT]
> 1. **Should we move `AppTheme` to `shared` module or duplicate it in `admin`?** Moving to `shared` is cleaner but touches the dependency graph. Duplicating means two files to maintain. I recommend moving to `shared` — confirm?

> [!IMPORTANT]
> 2. **How deep should the MainFrame decomposition go?** The plan proposes 6 new files. An alternative is to only extract the 3 renderers (3 new files) and leave the panel building methods in MainFrame. Which do you prefer?

> [!WARNING]
> 3. **Are the Python fix scripts (`fix.py`, `style_messenger.py`, etc.) safe to delete?** They appear to have been one-time patches. Confirm they're no longer needed.

---

## Verification Plan

### Automated Tests
```powershell
# After each phase, verify the project compiles
cd D:\Code\Java\test
powershell -ExecutionPolicy Bypass -File scripts\build.ps1 -SkipTests
```

### Manual Verification
After each phase, launch both apps to visually verify:
```powershell
# Client
powershell -ExecutionPolicy Bypass -File scripts\run.ps1 -SkipBuild -Mode hybrid -ServerHost 127.0.0.1 -ServerPort 61000 -Profile TestA -PrivatePort 50011

# Admin
powershell -ExecutionPolicy Bypass -File scripts\run-admin.ps1 -SkipBuild -ServerHost 127.0.0.1 -ServerPort 61000 -Profile adminTest
```

**Phase-specific checks:**
| Phase | What to verify |
|-------|---------------|
| 1 | Both apps launch with identical look as before |
| 2 | Admin sidebar has styled backgrounds, typed section headers, styled buttons |
| 3 | Client looks and behaves identically to pre-refactor |
| 4 | Message bubbles render with proper rounding, no visual seams |
| 5 | Login dialogs have dark theme, styled inputs, hover buttons |
| 6 | No stale Python scripts remain; clean compile |
