package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.*

interface AuthService {

    @POST("/login")
    fun login(data: LoginData): Call<LoginResponse>

    @POST("/register")
    fun register(data: RegistrationData): Call<Void>

}