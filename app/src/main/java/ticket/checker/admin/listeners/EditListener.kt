package ticket.checker.admin.listeners

/**
 * Created by Dani on 25.03.2018.
 */
interface EditListener<in T> {
    fun onEdit(editedObject : T)
}