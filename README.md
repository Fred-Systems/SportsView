# SportsViewer

Polished Android video hub for the official NBA, NFL, and MLB YouTube channels.

## Features
- NBA / NFL / MLB tabs
- Channel upload library loaded 50 videos at a time
- Only video IDs returned from the selected official channel's uploads playlist are offered to the player
- 10-second back/forward, seek bar, previous/next, fullscreen
- No general YouTube home/search/browser
- Developed by Fred-

## Official channel IDs
- NBA: UCWJ2lWNubArHWmf3FIHbfcQ
- NFL: UCDVYQ4Zhbm3S2dlz7P1xGg
- MLB: UCoLrcjPV5PbUrUyXq5mjc_A

## Build requirement
The app needs a YouTube Data API v3 key to list the channel videos. In GitHub, go to Settings -> Secrets and variables -> Actions and add a repository secret named YOUTUBE_API_KEY. Then run the Build APK workflow. Do not commit the key.

The workflow produces an unsigned release APK as a downloadable artifact.