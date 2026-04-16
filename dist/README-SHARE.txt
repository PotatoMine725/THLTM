WiFi Chat - Quick Start (Portable)
==================================

Package:
- WiFiChatClient-portable.zip

How to run:
1) Unzip WiFiChatClient-portable.zip
2) Open folder WiFiChatClient-portable
3) Double-click start-client.bat
4) Register/Login and send a message in room "General"

Use your own profile name (recommended on shared PCs):
- Open Command Prompt in that folder and run:
  start-client.bat MY_PROFILE_NAME

Troubleshooting:
- Cannot connect/login:
  Ask admin to check server status:
  systemctl status wifichat-server --no-pager
- Windows Firewall prompt:
  Allow network access for the app.
- Server IP changed:
  Ask admin for a new client package built with updated ServerHost.

