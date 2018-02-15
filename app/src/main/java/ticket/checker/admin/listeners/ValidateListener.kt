package ticket.checker.admin.listeners

/**
 * Created by Dani on 15.02.2018.
 */
interface ValidateListener {
    fun onValidated(validatedItemPosition : Int, isValidated : Boolean)
}