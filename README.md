# SportsView

<div align="center">

# 🏀 SportsView 🏈

### The focused sports-video hub for the official NBA, NFL & MLB channels

**Watch official sports videos in one polished, distraction-free app.**

[![GitHub](https://img.shields.io/badge/GitHub-Fred--Systems%2FSportsView-181717?logo=github)](https://github.com/Fred-Systems/SportsView)
[![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)](https://www.android.com/)
[![Release](https://img.shields.io/github/v/release/Fred-Systems/SportsView?display_name=tag)](https://github.com/Fred-Systems/SportsView/releases)

</div>

---

## 🏀🏈⚾ What is SportsView?

**SportsView** is a polished Android sports-video app created by **Fred-Systems**.

Instead of turning into a general YouTube browser, SportsView is deliberately focused on the official video feeds from three leagues:

- 🏀 **NBA**
- 🏈 **NFL**
- ⚾ **MLB**

Pick a league, find a video, and watch it. That's the idea.

The app combines a clean sports-focused interface with playback controls, search, multiple YouTube player methods, viewing modes, landscape playback, and an optional calculator disguise.

---

# ✨ Features

## 🏟️ Official Sports Video Libraries

### 🏀 NBA
Browse videos from the official NBA YouTube channel with search, video selection, playback controls, and navigation.

### 🏈 NFL
Browse official NFL videos with additional fallback mechanisms designed to keep the NFL library usable when one feed source is unavailable.

### ⚾ MLB
Browse videos from the official MLB YouTube channel in the same unified interface.

SportsView is **not a general YouTube browser**. The video library is restricted to the selected official sports channel.

---

## 🎬 Powerful Video Playback

SportsView gives you control over how videos are played.

### Player methods

Choose between:

**IFrame API**  
The full YouTube IFrame player integration.

**Direct Embed**  
A simpler direct YouTube embed that works differently from the API player.

**Privacy Embed**  
Uses YouTube's privacy-enhanced `youtube-nocookie.com` player.

Your selected playback method is saved on the device, so you don't have to choose it again every time.

### Playback controls

- ⏪ 10-second rewind
- ⏩ 10-second forward
- ⏮️ Previous video
- ⏭️ Next video
- 🎚️ Seek through the video
- ▶️ Play/pause through the YouTube player
- 🖥️ Fullscreen
- ↔️ Landscape viewing

If one playback method doesn't behave well on a particular device or network, you can switch to another method without changing the rest of SportsView.

---

# 📐 Viewing Modes

SportsView includes three viewer sizes:

### Compact
A smaller player that leaves more room for the video library and controls.

### Standard
The normal everyday SportsView layout.

### Cinema
A larger, more immersive viewing layout.

You can also rotate the viewer into **landscape mode** for a more traditional video-watching experience.

---

# 🔎 Search

Search within the selected official sports feed.

Search is tied to the current league rather than sending the user into unrestricted YouTube browsing.

---

# 🧮 Calculator Disguise

SportsView includes an optional calculator mode for users who want the launcher to appear as a calculator.

When enabled:

- The launcher can use the calculator identity and icon.
- The app opens directly into a functional calculator.
- Normal arithmetic works like a regular calculator.
- The SportsView app is not displayed from the calculator interface.
- A user-selected **4–12 digit code** can be configured.
- The code is entered through the calculator and confirmed with **=** to return to SportsView.

The calculator also includes an advanced mode.

### Advanced calculator

Advanced functions include:

- sin / cos / tan
- inverse trigonometric functions
- logarithms
- natural logarithm
- square root
- squares
- powers
- factorial
- reciprocal
- absolute value
- floor / ceiling
- π and e
- memory functions
- degree/radian options

### Calculator experience

- ☀️ Light theme by default
- 🌙 Optional dark theme
- ↩️ Undo/back behavior
- Function names shown on the display
- Results shown as calculations are performed
- Compact advanced-function overlay

---

# 🎨 Designed to Stay Flexible

SportsView is built so that the user can choose how they want to use it.

You can change:

- Player method
- Viewer size
- Landscape mode
- Calculator mode
- Calculator theme
- Other application settings

The app remembers appropriate user preferences locally on the device.

---

# 🔐 Privacy & API Design

SportsView does **not** bundle a YouTube API key inside the Android APK.

Where the YouTube Data API is needed for server-side feed generation, the key is kept as a GitHub Actions secret:

`YOUTUBE_API_KEY`

**API keys should never be committed to the repository.**

The app is designed around official channel content rather than unrestricted YouTube browsing.

---

# 🏆 Official Channels

| League | Official YouTube channel ID |
|---|---|
| 🏀 NBA | `UCWJ2lWNubArHWmf3FIHbfcQ` |
| 🏈 NFL | `UCDVYQ4Zhbm3S2dlz7P1xGg` |
| ⚾ MLB | `UCoLrcjPV5PbUrUyXq5mjc_A` |

---

# 🛠️ Technology

SportsView combines:

- Android
- Kotlin
- WebView
- HTML / CSS / JavaScript
- YouTube playback
- GitHub Actions
- Server-side feed support
- Native Android calculator functionality

The Android layer provides native capabilities such as:

- Orientation control
- Calculator launcher switching
- Update checking
- Native calculator activity
- WebView ↔ Android communication

The web interface provides the polished SportsView experience.

---

# 📁 Project Structure

```
SportsView/
├── app/
│   └── src/main/
│       ├── assets/       # SportsView web interface
│       ├── java/         # Kotlin Android code
│       └── res/          # Icons and Android resources
├── .github/
│   └── workflows/        # Build and feed workflows
├── docs/
│   └── index.html        # SportsView web version
└── README.md
```

---

# 🌐 Web Version

SportsView also has a browser-based version with the same sports-focused interface.

**Website:**  
https://fred-systems.github.io/SportsView/

The web version is designed to work directly in a browser rather than depending on the Android bridge.

---

# 📦 Download

Get the latest Android release from:

**https://github.com/Fred-Systems/SportsView/releases**

---

# 🚀 Building SportsView

The Android project can be built through GitHub Actions.

The repository's workflows handle the Android build and supporting feed tasks.

For workflows requiring the YouTube Data API, configure:

`YOUTUBE_API_KEY`

as a **GitHub Actions repository secret**.

Never place the key directly into source code or an APK.

---

# 👨‍💻 Developer

**Developed by Fred-Systems**

GitHub:  
https://github.com/Fred-Systems/SportsView

---

<div align="center">

### 🏀 Official sports video. One focused app.

**SportsView**

© Fred-Systems

</div>
