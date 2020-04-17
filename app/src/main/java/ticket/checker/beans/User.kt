package ticket.checker.beans

import java.io.Serializable
import java.time.OffsetDateTime
import java.util.*

data class UserDefinition(val name: String): Serializable
data class UserProfile(val id: UUID, val email: String, val name: String, val createdAt: OffsetDateTime): Serializable