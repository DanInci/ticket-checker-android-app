package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.*
import ticket.checker.extras.InviteStatus
import java.util.*

interface OrganizationService {

    // organization routes

    @POST("/organizations")
    fun createOrganization(@Body definition: OrganizationDefinition): Call<Organization>

    @GET("/organizations")
    fun getOrganizations(@Query("page") pageNumber: Int?, @Query("pageSize") pageSize: Int?): Call<List<OrganizationList>>

    @GET("/organizations/{organizationId}")
    fun getOrganizationById(@Path("organizationId") id: UUID): Call<Organization>

    @PUT("/organizations/{organizationId}")
    fun updateOrganizationById(@Path("organizationId") id: UUID, @Body definition: OrganizationDefinition): Call<Organization>

    @DELETE("/organizations/{organizationId}")
    fun deleteOrganizationById(@Path("organizationId") id: UUID): Call<Void>

    // organization invite routes

    @GET("/organizations/{organizationId}/invites")
    fun getOrganizationInvites(@Path("organizationId") id: UUID, @Query("page") pageNumber: Int?, @Query("pageSize") pageSize: Int?, @Query("status") status: InviteStatus?): Call<List<OrganizationInviteList>>

    @POST("/organizations/{organizationId}/invites")
    fun inviteIntoOrganization(@Path("organizationId") id: UUID, @Body invite: OrganizationInviteDefinition): Call<OrganizationInvite>

    @DELETE("/organizations/{organizationId}/invites/{inviteId}")
    fun deleteOrganizationInviteById(@Path("organizationId") id: UUID, @Path("inviteId") inviteId: UUID): Call<Void>

    @GET("/organizations/join")
    fun joinOrganizationByInviteCode(@Query("code") inviteCode: String): Call<Organization>

    @POST("/organizations/{organizationId}/invites/{inviteId}/accept")
    fun acceptInvite(@Path("organizationId") id: UUID, @Path("inviteId") inviteId: UUID): Call<Void>

    @POST("/organizations/{organizationId}/invites/{inviteId}/decline")
    fun declineInvite(@Path("organizationId") id: UUID, @Path("inviteId") inviteId: UUID): Call<Void>

    // organization members routes

    @GET("/organizations/{organizationId}/users")
    fun getOrganizationMembers(@Path("organizationId") id: UUID, @Query("page") pageNumber: Int?, @Query("pageSize") pageSize: Int?): Call<List<OrganizationMemberList>>

    @PUT("/organizations/{organizationId}/users/{userId}")
    fun updateOrganizationMemberById(@Path("organizationId") id: UUID, @Path("userId") userId: UUID, @Body definition: OrganizationMemberDefinition): Call<OrganizationMember>

    @DELETE("/organizations/{organizationId}/users/{userId}")
    fun deleteOrganizationMemberById(@Path("organizationId") id: UUID, @Path("userId") userId: UUID)

}