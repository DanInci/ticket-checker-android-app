package ticket.checker.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

/**
 * Created by Dani on 09.02.2018.
 */
interface NumbersService {

    @GET("/numbers/users")
    fun getUserNumbers(@Query("role") role : String?) : Call<Int>

    @GET("/numbers/tickets")
    fun getFilteredTicketNumbers(@Query("filter") filter : String) : Call<Array<Int>>

    @GET("/numbers/tickets")
    fun getTicketNumbers(@Header("validated") isValidated : Boolean) : Call<Int>
}