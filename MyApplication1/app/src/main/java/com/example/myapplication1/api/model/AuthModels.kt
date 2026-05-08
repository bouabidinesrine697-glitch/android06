package com.example.myapplication1

data class LoginRequest(
    val email:    String,
    val password: String
)


data class UserData(
    val id:         Int,
    val username:   String,
    val email:      String,
    val first_name: String,
    val last_name:  String
)
data class Trottinette(
    val id: Int,
    val QR_code: String,
    val model: String,
    val status: String,
    val price_per_minute: Double?,
    val battery: Int,
    val latitude: Double?,
    val longitude: Double?
)
data class StationDisp(
    val id: Int,
    val nom: String,
    val longitude: Double,
    val latitude: Double,
    val nombre_total: Int,
    val nombre_disponibles: Int,
    val trottinettes: List<Trottinette> = emptyList()
)
data class BookingRequest(
    val Trottinette: Int,
    val client: Int,
    val start_time: String,
    val end_time: String,
    val total_cost: Double
)

data class BookingResponse(
    val id: Int,
    val Trottinette: Int,
    val client: Int,
    val start_time: String,
    val end_time: String,
    val total_cost: Double
)
data class RegisterRequest(
    val firstName: String,

    val lastName: String,

    val email: String,

    val phone: String,

    val password: String
)

data class RegisterResponse(
    val status: String, // e.g., "success"

    val message: String, // e.g., "User registered successfully"

    val userId: Int?, // The ID assigned by the database

    val token: String? // Optional: JWT token for automatic login
)
data class LoginResponse(
    val message: String,
    val access:  String,
    val refresh: String,
    val client:  Client
)

data class Client(
    val id:            Int,
    val nom:           String,
    val prenom:        String,
    val email:         String,
    val telephone:     String,
    val ville:         String,
    val adresse:       String,
    val latitude:      Double,
    val longitude:     Double,
    val date_naissance:String
)
data class ReservationsResponse(
    val count:        Int,
    val reservations: List<ReservationDetail>
)

data class ReservationDetail(
    val id:                 Int,
    val client_id:          Int,
    val client_nom:         String,
    val client_prenom:      String,
    val client_email:       String,
    val trottinette_id:     Int,
    val trottinette_modele: String,
    val start_time:         String,
    val end_time:           String?,
    val duration_so_far:    String,
    val total_cost:         Double?,
    val status:             String
)
data class EndBookingRequest(
    val client_id: Int
)
data class EndBookingResponse(
    val message:     String,
    val reservation: EndBookingDetails
)

data class EndBookingDetails(
    val id:                 Int,
    val client_id:          Int,
    val client_nom:         String,
    val client_prenom:      String,
    val trottinette_id:     Int,
    val trottinette_modele: String,
    val start_time:         String,
    val end_time:           String,
    val duration:           String,
    val total_cost:         Double,
    val status:             String
)