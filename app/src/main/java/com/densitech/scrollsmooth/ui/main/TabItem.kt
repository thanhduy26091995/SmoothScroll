package com.densitech.scrollsmooth.ui.main

import com.densitech.scrollsmooth.R

sealed class Screen(val route: String, val title: String, val resourceId: Int) {
    data object Home : Screen("home", "Home", R.drawable.ic_home_24)
    data object Search : Screen("search", "Search", R.drawable.ic_search_24)
    data object Add : Screen("add", "Add", R.drawable.ic_add_circle_outline_24)
    data object Profile : Screen("profile", "Profile", R.drawable.ic_person_24)
    data object VideoTransformation : Screen("video_transformation", "Video Transformation", 0)
}