package ticket.checker.services

import retrofit2.Call
import retrofit2.http.GET
import ticket.checker.beans.User

/**
 * Created by Dani on 24.01.2018.
 */
interface UserService {

    @GET("login")
    fun getUser() : Call<User>

}