package ticket.checker.beans

import ticket.checker.extras.InviteStatus
import ticket.checker.extras.OrganizationRole
import java.io.Serializable
import java.util.*

data class OrganizationDefinition(val name: String): Serializable
data class OrganizationList(val id: UUID, val name: String, val membership: Membership, val createdAt: Date): Serializable {
    fun toOrganization(): Organization = Organization(id, name, membership, createdAt)
}
data class Organization(val id: UUID, val name: String, val membership: Membership, val createdAt: Date): Serializable {
    fun toOrganizationList(): OrganizationList = OrganizationList(id, name, membership, createdAt)
}
data class Membership(val role: OrganizationRole, val joinedAt: Date): Serializable

data class OrganizationInviteDefinition(val email: String): Serializable
data class OrganizationInviteList(val id: UUID, val organizationId: UUID, val organizationName: String, val status: InviteStatus, val invitedAt: Date): Serializable
data class OrganizationInvite(val id: UUID, val organizationId: UUID, val organizationName: String, val email: String, val status: InviteStatus, val answeredAt: Date?, val invitedAt: Date): Serializable

data class OrganizationMemberDefinition(val role: OrganizationRole): Serializable
data class OrganizationMemberList(val organizationId: UUID, val userId: UUID, val name: String, val role: OrganizationRole, val joinedAt: Date): Serializable
data class OrganizationMember(val organizationId: UUID, val organizationName: String, val userId: UUID, val name: String, val role: OrganizationRole, val joinedAt: Date, val soldTicketsNo: Int, val validatedTicketsNo: Int): Serializable {
    fun withPretendedRole(pretendedRole: OrganizationRole) = OrganizationMemberWithPretendedRole(organizationId, organizationName, userId, name, role, pretendedRole, joinedAt, soldTicketsNo, validatedTicketsNo)
    fun toOrganizationMemberList(): OrganizationMemberList = OrganizationMemberList(organizationId, userId, name, role, joinedAt)
}
data class OrganizationMemberWithPretendedRole(val organizationId: UUID, val organizationName: String, val userId: UUID, val name: String, val role: OrganizationRole, val pretendedRole: OrganizationRole, val joinedAt: Date, val soldTicketsNo: Int, val validatedTicketsNo: Int)