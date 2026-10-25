# tvOS Launcher custom source overlay

This directory contains original Java helper source written for a separate custom build of the supplied tvOS Launcher APK. It is provided as a reference overlay; it is not part of CleanYourTV's `com.gathin.tvhome` launcher and is not a standalone build project.

## Source

- `src/TvFeatures.java` — app visibility controls, app-focus gradients and launcher feature helpers.
- `src/MusicCardView.java` — remote-focusable music playback card.

The overlay was compiled against the separately supplied tvOS Launcher base and its generated Android resources. That base APK, its decompiled framework code, signing material, and device-specific files are not included. These helpers alone are not sufficient to reproduce the APK; integration requires the matching base source/resources and patch points.

The latest source adds a smooth focused-app gradient that follows the launcher's `appearance.dark` setting: light mode fades towards a lighter gradient, while dark mode fades towards a darker gradient. This source update has compiled, but it has not yet been signed or installed on the TV.

## Provenance and limits

This folder contains only the custom Java helper files. It does not contain upstream CleanYourTV code, third-party smali, the built APK, screenshots, TV identifiers, network addresses, backups, or signing credentials. The earlier custom build was installed and visually checked on the user's TV; that does not validate the pending gradient-mode update.
