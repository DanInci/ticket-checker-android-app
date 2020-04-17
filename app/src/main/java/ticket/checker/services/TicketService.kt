package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.Ticket
import ticket.checker.beans.TicketDefinition
import ticket.checker.beans.TicketList
import ticket.checker.beans.TicketUpdateDefinition
import ticket.checker.extras.TicketCategory
import java.util.*

interface TicketService {

    @POST("/organizations/{organizationId}/tickets")
    fun createTicketForOrganization(@Path("organizationId") id: UUID, @Body definition: TicketDefinition): Call<Ticket>

    @GET("/organizations/{organizationId}/tickets")
    fun getTicketsForOrganization(@Path("organizationId") id: UUID, @Query("page") pageNumber: Int?, @Query("pageSize") pageSize: Int?, @Query("category") ticketCategory: TicketCategory?, @Query("userId") userId: UUID?, @Query("search") searchValue: String?): Call<List<TicketList>>

    @GET("/organizations/{organizationId}/tickets/{ticketId}")
    fun getTicketById(@Path("organizationId") id: UUID, @Path("ticketId") ticketId: UUID): Call<Ticket>

    @PUT("/organizations/{organizationId}/tickets/{ticketId}")
    fun updateTicketById(@Path("organizationId") id: UUID, @Path("ticketId") ticketId: UUID, @Body definition: TicketUpdateDefinition): Call<Ticket>

    @POST("/organizations/{organizationId}/tickets/{ticketId}/validate")
    fun validateTicketById(@Path("organizationId") id: UUID, @Path("ticketId") ticketId: UUID): Call<Ticket>

    @POST("/organizations/{organizationId}/tickets/{ticketId}/invalidate")
    fun invalidateTicketById(@Path("organizationId") id: UUID, @Path("ticketId") ticketId: UUID): Call<Ticket>

    @DELETE("/organizations/{organizationId}/tickets/{ticketId}")
    fun deleteTicketById(@Path("organizationId") id: UUID, @Path("ticketId") ticketId: UUID): Call<Void>

}