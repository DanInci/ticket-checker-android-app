package ticket.checker.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query
import ticket.checker.beans.Statistic

/**
 * Created by Dani on 09.02.2018.
 */
interface StatisticsService {

    @GET("/statistics/numbers/users")
    fun getUserNumbers(@Query("role") role : String?) : Call<Int>

    @GET("/statistics/numbers/tickets")
    fun getFilteredTicketNumbers(@Query("filter") filter : String) : Call<Array<Int>>

    @GET("/statistics/numbers/tickets")
    fun getTicketNumbers(@Header("validated") isValidated : Boolean) : Call<Int>

    @GET("/statistics/tickets")
    fun getTicketStatisticsForInterval(@Query("type") type : String, @Query("interval") interval : String, @Query("size") size : Int?) : Call<List<Statistic>>
}