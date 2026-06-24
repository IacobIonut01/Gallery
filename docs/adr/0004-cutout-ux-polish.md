# 0004-cutout-ux-polish

We implemented several user experience (UX) refinements, performance optimizations, and integrations to finalize the subject cutout feature.

## Context

*   To ensure the subject cutout feature is fully functional and feels like a premium gallery utility, we needed state preservation (undo/redo), visual feedback (contour glow, active state, processing states), and custom ML engine options (BFS refinement).
*   During initial user testing, several small UX frictions and missing features were identified, such as:
    1. Swiping between images in the pager would trigger accidentally during interactive point placement.
    2. Lack of way to clear the current session points without leaving cutout mode.
    3. The raw SAM cutout outline lacked feedback when active.
    4. The settings manager screen had no visual representation/preview of the cutout capabilities.

## Decision

We will implement the following UX, layout, and post-processing improvements:

1.  **Undo/Redo History Stack:** A point prompt history stack supporting both undo and redo. We cache the immediate previous result to allow instant toggling between adjacent points in the history stack, preventing redundant inference runs.
2.  **Animated Contour Glow:** Draw a pulsing white outline around the segmented subject in active cutout mode. This is done via an 8-direction bitmap offset rendering on a Canvas, driven by an infinite transition.
3.  **Breadth-First Search (BFS) Mask Refinement:** Enhance post-processing by running a Morphological BFS cleanup pass that fills small interior holes and removes small isolated foreground island masks, automatically scaling the area thresholds dynamically based on the cropped bounding box.
4.  **Reset Capability:** Add a Reset button to clear all prompt points while keeping the user in Active Cutout Mode.
5.  **Pager Scroll Control:** Hoist the active cutout state to block parent ViewPager swiping/paging gestures when the cutout session is active.
6.  **Settings Preview Card:** Render an animated `CutoutPreview` card in the AI Smart features manager screen (`AIModelsManagerScreen`), showing a mock cutout transition effect.

## Consequences

*   **Robust Interaction:** Point placement mistakes are easily reversible via the undo/redo history.
*   **Polished Presentation:** The glowing outline makes the active selection clearly feel alive and separated from the background.
*   **Clean Masks:** BFS cleanup removes isolated floating noise pixels and corrects internal segmentation gaps.
*   **Intuitive Controls:** Reset and Pager Disable prevent accidental navigations and UI frustrations.
