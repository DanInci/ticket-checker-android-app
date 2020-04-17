package ticket.checker.extras

import ticket.checker.R
import java.io.Serializable

enum class OrganizationRole(val role : String, val colorResource : Int ) : Serializable {
    OWNER("Organization Owner", R.color.noRed),
    ADMIN("Admin", R.color.yesGreen),
    PUBLISHER("Publisher", R.color.deepPurple),
    VALIDATOR("Validator", R.color.materialYellow),
    USER("User", R.color.darkerGrey);

    companion object {
        fun from(role : String) : OrganizationRole? = when(role) {
            "OrganizationOwner" -> OWNER
            "Admin" -> ADMIN
            "Publisher" -> PUBLISHER
            "Validator" -> VALIDATOR
            "User" -> USER
            else -> null
        }

        fun to(role : OrganizationRole) : String = when(role) {
            OWNER -> "OrganizationOwner"
            ADMIN -> "Admin"
            PUBLISHER -> "Publisher"
            VALIDATOR -> "Validator"
            USER -> "User"
        }
    }
}