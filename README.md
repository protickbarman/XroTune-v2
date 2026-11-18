# XroTune

Modern Android music streaming & offline library app built with **Kotlin**, **Jetpack Compose (Material 3)**, **Media3 ExoPlayer**, **Room**, **Retrofit + Moshi**, and **Coil**.

- **Package ID:** `com.xrotune.app`
- **Backend API Base URL:** `https://xro-yt-api.onrender.com` (documented at `/api/doc`)

## Key Features

- **Borderless iOS-Style Studio UI:** Zero-border tinted dark surfaces, soft elevation shadows, zero-ripple tactile scale interactions (`Modifier.iosClickable`), crisp spring animations, and a cinematic launch reveal.
- **Floating Popup Player & Mini Player:** Bottom-anchored rounded popup sheet with a Cross (`X`) minimize button, top drag handle pill, swipe-down gesture to collapse into the persistent Mini Player, embedded **Lyrics** tab (`/api/music/lyrics`), and **Radio & Queue** tab (`/api/music/radio`).
- **Full YouTube Music (`/api/music/*`) Integration:**
  - Home personalized shelves (`/api/music/home`) & Trending charts (`/api/music/trending`)
  - Categorized music search (`songs`, `videos`, `albums`, `artists`, `playlists`) with continuation paging (`/api/music/search`)
  - Interactive Artist (`/api/music/artist`, `/api/music/artist/songs`), Album (`/api/music/album`), and Playlist (`/api/music/playlist`) sheets
  - Format & bitrate inspector (`/api/info`, `/api/link`, `/api/music/song`)
- **Verified Offline Playback:** Local downloads via `/api/download` with selectable audio quality tiers, Room persistence, strict `videoId` stream verification, and instant offline playback priority.
- **Local `.xrotune` Profile & Settings Backup:** Export/import your profile, audio preferences, and download settings to a `.xrotune` file.
- **Server Health, `/api/doc` & Live Logs:** Real-time `/health` telemetry, live API & playback logs viewer, and searchable `/api/doc` endpoint explorer inside Settings.

## Building & Testing

```bash
gradle :app:assembleDebug
gradle :app:testDebugUnitTest
```
