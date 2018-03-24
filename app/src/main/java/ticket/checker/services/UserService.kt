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

    @GET("/users/{loggedInUserId}")
    fun getUsersById(@Path("loggedInUserId") userId : Long) : Call<User>

    @GET("/users/{loggedInUserId}/sold")
    fun getSoldTicketsByUserId(@Path("loggedInUserId") userId: Long) : Call<List<Ticket>>

    @GET("/users/{loggedInUserId}/validated")
    fun getValidatedTicketsByUserId(@Path("loggedInUserId") userId: Long) : Call<List<Ticket>>

    @POST("/users")
    fun createUser(@Body user: User) : Call<User>

    @POST("/users/{loggedInUserId}")
    fun editUser(@Path("loggedInUserId") userId : Long, @Body user: User) : Call<User>

    @DELETE("/users/{loggedInUserId}")
    fun deleteUserById(@Path("loggedInUserId") userId: Long) : Call<Void>

}