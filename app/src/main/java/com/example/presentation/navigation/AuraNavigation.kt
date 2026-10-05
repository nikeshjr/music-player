package com.example.presentation.navigation

/**
 * AuraNavigation: Destination definitions for Aura Music.
 */
sealed class AuraDestination(val route: String) {
    data object Library : AuraDestination("library")
    data object Search : AuraDestination("search")
    data object Playlists : AuraDestination("playlists")
    data object NowPlaying : AuraDestination("now_playing")
    data object Lyrics : AuraDestination("lyrics")
    data object Queue : AuraDestination("queue")
    data object Appearance : AuraDestination("appearance")
    data object AudioInspector : AuraDestination("audio_inspector")
    data object IslandCalibration : AuraDestination("island_calibration")
}

enum class NavigationTab(val title: String, val route: String) {
    LIBRARY("Library", "library"),
    DASHBOARD("Dashboard", "dashboard"),
    APPEARANCE("Styling", "appearance")
}
