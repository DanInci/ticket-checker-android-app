package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.Ticket

/**
 * Created by Dani on 25.01.2018.
 */
interface TicketService {

    @GET("/tickets")
    fun validateTicket(@Query("validate") ticketId : String) : Call<Void>

    @POST("/tickets")
    fun createTicket(@Body ticket : Ticket) : Call<Void>

    @DELETE("/tickets/{ticketId}")
    fun deleteTicket(@Path("ticketId") ticketId : String) : Call<Void>
}