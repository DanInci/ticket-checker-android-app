package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.*

interface AuthService {

    @POST("login")
    fun login(@Body data: LoginData): Call<LoginResponse>

    @POST("register")
    fun register(@Body data: RegistrationData): Call<Void>

    @POST("verify")
    fun verifyAccount(@Query("code") verificationCode: String): Call<Void>

}