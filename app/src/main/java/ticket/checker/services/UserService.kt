package ticket.checker.services

import retrofit2.Call
import retrofit2.http.*
import ticket.checker.beans.*
import ticket.checker.extras.InviteStatus
import java.util.*

interface UserService {

    @GET("/users/{userId}")
    fun getUserById(@Path("userId") id: UUID): Call<UserProfile>

    @GET("/users/{userId}/invites")
    fun getUserInvites(@Path("userId") id: UUID, @Query("page") pageNumber: Int?, @Query("pageSize") pageSize: Int?, @Query("status") status: InviteStatus?): Call<List<OrganizationInviteList>>

    @PUT("/users/{userId}")
    fun updateUserById(@Path("userId") id: UUID, @Body definition: UserDefinition): Call<UserProfile>

    @DELETE("/users/{userId}")
    fun deleteUserById(@Path("userId") id: UUID): Call<Void>

}