package ticket.checker.admin.listeners

/**
 * Created by Dani on 11.02.2018.
 */
interface ListChangeListener<in T> {
    fun onAdd(addedObject : T)
    fun onEdit(editedObject : T, editedObjectPosition : Int)
    fun onRemove(removedItemPosition : Int)
}