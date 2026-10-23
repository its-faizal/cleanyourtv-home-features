# Home features in this local fork

This folder is a local fork of [Gathin23/CleanYourTV](https://github.com/Gathin23/cleanyourtv), kept separate from the upstream checkout.

## App controls

The launcher already supports pinned dock favourites, D-pad reordering, and removing apps from Home. This fork turns on the App Library, its Home-selection mode, the tile action menu, and the reorder button by default:

- Open **Apps** to add or remove Home tiles.
- Hold **OK** on a tile to move it, add it to the dock, remove it from Home, or open App info.
- Use the reorder control to move tiles with the D-pad.

Removing an app from Home only removes its launcher tile; it does not disable or uninstall the app.

## Music visualiser

Open **Launcher settings → Music visualiser** and enable it. Android asks for its `RECORD_AUDIO` permission because the visualiser reads the TV's output mix. It does not open the microphone, save audio, or send data over a network. Capture starts only while Home is visible and is released when Home pauses.

The visualiser is off until explicitly enabled. If the TV firmware blocks output-mix visualisation, the rest of the launcher continues to work normally.

## Build

Build the launcher from source with JDK 17, Android SDK platform 34, and build-tools 34.0.0 from the repository root with `launcher/gradlew.bat --no-daemon assembleRelease`. This fork has not been installed on a TV.
