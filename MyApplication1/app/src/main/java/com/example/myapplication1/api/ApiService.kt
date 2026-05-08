package com.example.myapplication1

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("client/clients/login/")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<LoginResponse>

    @GET("trottinettes/TrottinetteList/")
    suspend fun getListtrottinettes(): Response<List<Trottinette>>

    @GET("zone/zones-disponible/")
    suspend fun getStations(): Response<List<StationDisp>>

    @POST("trottinettes/TrottinetteBookingAdd/")
    suspend fun addBooking(
        @Body request: BookingRequest
    ): Response<BookingResponse>

    @POST("clients/add/")
    suspend fun register(
        @Body client: RegisterRequest
    ): Response<RegisterResponse>
    @GET("trottinettes/trottinettes/{client_id}/retourner/")
    suspend fun getReservations(
        @Path("client_id") clientId: Int
    ): Response<ReservationsResponse>
    @PUT("trottinettes/trottinettes/booking/{booking_id}/end/")
    suspend fun endBooking(
        @Path("booking_id") bookingId: Int,
        @Body request: EndBookingRequest
    ): Response<EndBookingResponse>
}