# Walkthrough - Fix Floating Bubble Budget & Capacity Edit Crash

## Problem
When clicking "Edit" on the "Verfügbares Inventar & Kapital" card in the floating bubble overlay, modifying silver budget or carry capacity and saving/closing caused the app to crash due to Jetpack Compose `Dialog`s attempting window focus changes and token management inside a `WindowManager` overlay view configured with `FLAG_NOT_FOCUSABLE`.

## Solution
- Replaced the standalone Jetpack Compose `Dialog` within `FloatingBubbleService.kt` with an inline expandable Card interface inside the bubble overlay view.
- Ensured that toggling edit mode and saving correctly manages soft keyboard hiding and window focus states without throwing window token or focus exceptions.
- Successfully built and verified the app via `app:assembleDebug`.
