package ticket.checker.beans

import java.io.Serializable
import java.util.*

data class TicketDefinition(val id: String, val soldTo: String?, val soldToBirthDay: Date?, val soldToTelephone: String?): Serializable
data class TicketList(val id: String, val organizationId: UUID, val soldTo: String?, val soldAt: Date, val validatedAt: Date?): Serializable
data class TicketUpdateDefinition(val soldTo: String?, val soldToBirthday: Date?, val soldToTelephone: String?): Serializable
data class Ticket(val id: String, val organizationId: UUID, val soldTo: String?, val soldToBirthday: Date?, val soldToTelephone: String?, val soldBy: UserProfile?, val soldByName: String?, val soldAt: Date, val validatedBy: UserProfile?, val validatedByName: String?, val validatedAt: Date?): Serializable {
    fun toTicketList(): TicketList = TicketList(id, organizationId, soldTo, soldAt, validatedAt)
}