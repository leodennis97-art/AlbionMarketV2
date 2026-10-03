# Implementation Plan - Bubble Size Customization for Vertical and Horizontal Views

Remove the old scale/zoom (+/-) buttons from the bubble header, and introduce separate width and height settings saved independently for vertical (portrait) and horizontal (landscape) orientations.

## Proposed Changes

### [AppPreferences.kt](file:///C:/Users/Dennis/Desktop/AlbionDataHack/AlbionMarketV2-Source/app/src/main/java/com/example/albionmarketv2/AppPreferences.kt)
- Add preferences for:
  - `bubbleWidthPortrait`: Int (default e.g. 240)
  - `bubbleHeightPortrait`: Int (default e.g. 200)
  - `bubbleWidthLandscape`: Int (default e.g. 340)
  - `bubbleHeightLandscape`: Int (default e.g. 140)
- (Optionally keep or deprecate `bubbleScale` if needed, but remove its use in the bubble dimensions).

### [FloatingBubbleService.kt](file:///C:/Users/Dennis/Desktop/AlbionDataHack/AlbionMarketV2-Source/app/src/main/java/com/example/albionmarketv2/FloatingBubbleService.kt)
- Remove `bubbleScale`, zoom out (`-`), zoom in (`+`) buttons from the bubble header.
- Replace or update sizing logic to use `bubbleWidthPortrait`, `bubbleHeightPortrait`, `bubbleWidthLandscape`, and `bubbleHeightLandscape` (respecting `isLandscape`, `isMaximized`, `isExpanded`, etc., or allowing direct width/height values/sliders).
- Update `BubbleSettingsTab` to let users adjust width and height for both Vertical and Horizontal orientations.

## Verification Plan
- Build project successfully using gradle build.
