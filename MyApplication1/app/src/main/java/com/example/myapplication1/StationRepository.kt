package com.example.myapplication1

import kotlin.random.Random
import kotlin.math.*

data class Station(
    val id: Int,
    val name: String,
    val lat: Double,
    val lng: Double,
    val bikes: Int,
    val slots: Int,
    val address: String = ""
) {
    val isAvailable: Boolean get() = bikes > 0
    val status: StationStatus get() = when {
        bikes == 0 -> StationStatus.EMPTY
        bikes <= 2 -> StationStatus.LOW
        else       -> StationStatus.OK
    }
}

enum class StationStatus { OK, LOW, EMPTY }

object StationRepository {

    private val stationNames = listOf(
        "Station Centrale",
        "Station Nord",
        "Station Sud",
        "Station Est",
        "Station Ouest",
        "Station Marché",
        "Station Université",
        "Station Hôpital",
        "Station Gare",
        "Station Parc",
        "Station Mairie",
        "Station Plage",
        "Station Stade",
        "Station Commerce",
        "Station Résidence"
    )

    fun getStations(
        userLat: Double = 36.8065,
        userLng: Double = 10.1815
    ): List<Station> {

        return stationNames.mapIndexed { index, name ->

            // ✅ Distance aléatoire entre 100m et 1000m
            val distanceMeters = Random.nextDouble(100.0, 1000.0)

            // ✅ Angle aléatoire 360°
            val angleDeg = Random.nextDouble(0.0, 360.0)
            val angleRad = Math.toRadians(angleDeg)

            // ✅ Conversion mètres → degrés
            // 1° lat ≈ 111,000m
            // 1° lng ≈ 111,000m * cos(lat)
            val dLat = distanceMeters / 111_000.0
            val dLng = distanceMeters / (111_000.0 * cos(Math.toRadians(userLat)))

            val finalLat = userLat + dLat * cos(angleRad)
            val finalLng = userLng + dLng * sin(angleRad)

            val bikes = Random.nextInt(0, 13)
            val slots = 12 - bikes

            Station(
                id      = index + 1,
                name    = name,
                lat     = finalLat,
                lng     = finalLng,
                bikes   = bikes,
                slots   = slots,
                address = name
            )
        }
    }
}