package ticket.checker.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ticket.checker.beans.TicketsStatistic
import ticket.checker.extras.IntervalType
import ticket.checker.extras.TicketCategory
import java.time.LocalDateTime
import java.util.*

interface StatisticsService {

    @GET("/statistics/organizations/{organizationId}/tickets")
    fun getTicketsStatistics(@Path("organizationId") id: UUID, @Query("category") ticketCategory: TicketCategory, @Query("interval") intervalType: IntervalType, @Query("size") size: Int?, @Query("until") until: LocalDateTime?): Call<List<TicketsStatistic>>

}