package ticket.checker.beans

import ticket.checker.extras.InviteStatus
import ticket.checker.extras.OrganizationRole
import java.io.Serializable
import java.time.OffsetDateTime
import java.util.*

data class OrganizationDefinition(val name: String): Serializable
data class OrganizationList(val id: UUID, val name: String, val createdAt: OffsetDateTime): Serializable
data class Organization(val id: UUID, val name: String, val createdAt: OffsetDateTime): Serializable

data class OrganizationInviteDefinition(val email: String): Serializable
data class OrganizationInviteList(val id: UUID, val status: InviteStatus, val invitedAt: OffsetDateTime): Serializable
data class OrganizationInvite(val id: UUID, val email: String, val status: InviteStatus, val answeredAt: OffsetDateTime?, val invitedAt: OffsetDateTime): Serializable

data class OrganizationMemberDefinition(val role: OrganizationRole): Serializable
data class OrganizationMemberList(val userId: UUID, val name: String, val role: OrganizationRole, val joinedAt: OffsetDateTime): Serializable
data class OrganizationMember(val userId: UUID, val name: String, val role: OrganizationRole, val joinedAt: OffsetDateTime): Serializable