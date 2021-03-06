package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.Ticket

/**
 * Created by Dani on 25.01.2018.
 */
interface TicketService {

    @GET("/tickets")
    fun getTickets(@Query("type") type : String?,@Query("value") value : String?, @Query("page") page : Int?, @Query("size") size : Int?) : Call<List<Ticket>>

    @GET("/tickets/{ticketId}")
    fun getTicketById(@Path("ticketId") ticketId : String) : Call<Ticket>

    @POST("/tickets")
    fun createTicket(@Body ticket : Ticket) : Call<Ticket>

    @POST("/tickets/{ticketId}")
    fun editTicket(@Path("ticketId") ticketId : String, @Body ticket : Ticket) : Call<Ticket>

    @POST("/tickets/validate/{ticketId}")
    fun validateTicket(@Header("validate") validate : Boolean, @Path("ticketId") ticketId : String) : Call<Ticket>

    @DELETE("/tickets/{ticketId}")
    fun deleteTicketById(@Path("ticketId") ticketId : String) : Call<Void>
}