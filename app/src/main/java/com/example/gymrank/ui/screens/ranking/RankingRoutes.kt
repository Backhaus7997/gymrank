package com.example.gymrank.ui.screens.ranking

import androidx.navigation.NavType
import androidx.navigation.navArgument

object RankingRoutes {

    const val Details = "rank/details" +
            "?gymName={gymName}" +
            "&gymLocation={gymLocation}" +
            "&periodLabel={periodLabel}" +
            "&myPosition={myPosition}" +
            "&myPoints={myPoints}"

    val detailsArgs = listOf(
        navArgument("gymName") { type = NavType.StringType; defaultValue = "" },
        navArgument("gymLocation") { type = NavType.StringType; defaultValue = "" },
        navArgument("periodLabel") { type = NavType.StringType; defaultValue = "" },
        navArgument("myPosition") { type = NavType.IntType; defaultValue = 0 },
        navArgument("myPoints") { type = NavType.IntType; defaultValue = 0 }
    )

    fun detailsRoute(
        gymName: String,
        gymLocation: String,
        periodLabel: String,
        myPosition: Int,
        myPoints: Int
    ): String {
        // ojo: encode simple para evitar romper la URL con espacios
        fun enc(s: String) = java.net.URLEncoder.encode(s, "UTF-8")

        return "rank/details" +
                "?gymName=${enc(gymName)}" +
                "&gymLocation=${enc(gymLocation)}" +
                "&periodLabel=${enc(periodLabel)}" +
                "&myPosition=$myPosition" +
                "&myPoints=$myPoints"
    }
}