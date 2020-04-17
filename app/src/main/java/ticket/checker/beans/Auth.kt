package ticket.checker.beans

import java.io.Serializable

data class RegistrationData(val email: String, val password: String, val name: String): Serializable
data class LoginData(val email: String, val password: String): Serializable
data class LoginResponse(val token: String, val profile: UserProfile)