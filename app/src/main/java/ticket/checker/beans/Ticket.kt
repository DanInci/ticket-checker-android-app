package ticket.checker.beans

import java.io.Serializable
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*

data class TicketDefinition(val id: String, val soldTo: String?, val soldToBirthDay: LocalDate?, val soldToTelephone: String?): Serializable
data class TicketList(val id: String, val soldAt: OffsetDateTime, val validatedAt: OffsetDateTime?): Serializable
data class TicketUpdateDefinition(val soldTo: String?, val soldToBirthDay: LocalDate?, val soldToTelephone: String?): Serializable
data class Ticket(val id: String, val organizationId: UUID, val soldTo: String?, val soldToBirthDay: LocalDate?, val soldToTelephone: String?, val soldBy: UserProfile?, val soldByName: String?, val soldAt: OffsetDateTime, val validatedBy: UserProfile?, val validatedByName: String?, val validatedAt: OffsetDateTime?): Serializable