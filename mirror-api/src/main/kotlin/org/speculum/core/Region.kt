package org.speculum.core

/**
 * Mirrors MagicMirror's screen regions. Modules declare a position and the
 * layout engine places them in the matching region of the fullscreen dashboard.
 */
enum class Region {
    TOP_LEFT, TOP_CENTER, TOP_RIGHT,
    UPPER_THIRD, MIDDLE_CENTER, LOWER_THIRD,
    BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
    BOTTOM_BAR, FULLSCREEN_ABOVE, FULLSCREEN_BELOW
}