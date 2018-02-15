package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.Ticket
import ticket.checker.beans.User

/**
 * Created by Dani on 24.01.2018.
 */
interface UserService {

    @GET("/login")
    fun getUser() : Call<User>

    @GET("/users")
    fun getUsers(@Query("role") role : String?, @Query("page") page : Int?, @Query("size") size : Int?) : Call<List<User>>

    @GET("/users/{userId}")
    fun getUsersById(@Path("userId") userId : Long) : Call<User>

    @GET("/users/{userId}/sold")
    fun getSoldTicketsByUserId(@Path("userId") userId: Long) : Call<List<Ticket>>

    @GET("/users/{userId}/validated")
    fun getValidatedTicketsByUserId(@Path("userId") userId: Long) : Call<List<Ticket>>

    @POST("/users")
    fun createUser(@Body user: User) : Call<User>

    @DELETE("/users/{userId}")
    fun deleteUserById(@Path("userId") userId: Long) : Call<Void>

}