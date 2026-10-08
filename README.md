# SportsView

A polished Android video hub built by **Fred-Systems** for watching the official **NBA, NFL, and MLB** YouTube channels in one focused app.

[![Repository](https://img.shields.io/badge/GitHub-Fred--Systems%2FSportsView-181717?logo=github)](https://github.com/Fred-Systems/SportsView)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Release](https://img.shields.io/github/v/release/Fred-Systems/SportsView?display_name=tag)](https://github.com/Fred-Systems/SportsView/releases)

## 🏀 🏈 ⚾ What is SportsView?

SportsView keeps the official NBA, NFL, and MLB video feeds together in a clean, distraction-free interface.

It is designed around the official channels—not general YouTube browsing—so the app stays focused on sports video.

### Features

- 🏀 **NBA** official video library
- 🏈 **NFL** official video library with fallback sources
- ⚾ **MLB** official video library
- ▶️ YouTube playback with multiple player methods
- 🔄 **IFrame API, Direct Embed, and Privacy Embed** player options
- 💾 Player-method preference persists on the device
- ⏪ 10-second rewind
- ⏩ 10-second forward
- ⏮️ Previous video
- ⏭️ Next video
- 🎚️ Seek/progress controls
- 🖥️ Fullscreen playback
- ↔️ Landscape viewing mode
- 📐 Compact, Standard, and Cinema viewer sizes
- 🔎 Search within the selected official channel feed
- 📱 Designed for Android/WebView environments
- 🧮 Optional calculator disguise with a fully functional calculator
- 🔐 Calculator launcher protected by a user-selected 4–12 digit code
- 🧠 Advanced calculator mode with scientific functions
- 🌙 Calculator light/dark themes
- 🔄 Built-in update checking
- 🛡️ No YouTube API key is bundled into the APK

## 🎬 Official channels

SportsView is built around these official channel IDs:

| League | Official channel |
|---|---|
| NBA | `UCWJ2lWNubArHWmf3FIHbfcQ` |
| NFL | `UCDVYQ4Zhbm3S2dlz7P1xGg` |
| MLB | `UCoLrcjPV5PbUrUyXq5mjc_A` |

The app uses the official uploads feeds/data sources and does not provide a general-purpose YouTube browser.

## 🧮 Calculator mode

SportsView can optionally disguise its launcher as a normal calculator.

The calculator supports:

- Addition, subtraction, multiplication, and division
- Percentages and decimals
- Undo/backspace
- Scientific functions
- Trigonometry
- Logarithms
- Powers and roots
- Factorials
- Constants such as π and e
- Memory functions
- Light/dark calculator themes

The calculator remains the calculator until the configured code is entered and confirmed with **=**.

## 🎨 Player options

If one YouTube playback method has trouble on a particular device/network, SportsView lets the user switch methods without changing the rest of the app:

1. **IFrame API**
2. **Direct Embed**
3. **Privacy Embed**

The selected method is saved locally on the device.

## 🛠️ Building

This repository is built with Gradle and GitHub Actions.

The YouTube Data API key, where required by the feed-building workflow, belongs in a GitHub Actions repository secret named:

`YOUTUBE_API_KEY`

**Never commit an API key to the repository or APK.**

The GitHub Actions workflow builds the Android APK and can publish releases.

## 📦 Releases

Download the latest APK from the project's GitHub Releases page:

**https://github.com/Fred-Systems/SportsView/releases**

## 🌐 Project website

The project website is included in the repository under `docs/` and is intended for GitHub Pages.

After enabling GitHub Pages for the repository with:

- **Source:** GitHub Actions
- **Branch:** main

the site will be available at:

**https://fred-systems.github.io/SportsView/**

## 📁 Project structure

```
app/
├── src/main/
│   ├── assets/          # SportsView web interface
│   ├── java/             # Android activities and native bridge
│   └── res/              # Android resources and launcher icons
.github/
└── workflows/            # Android build and feed workflows
docs/
└── index.html            # Project website
```

## 👨‍💻 Developer

**Developed by Fred-Systems**

Repository:  
https://github.com/Fred-Systems/SportsView

---

© Fred-Systems. SportsView is an independent project focused on official NBA, NFL, and MLB channel content.
