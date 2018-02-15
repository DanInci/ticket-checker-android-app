package ticket.checker.admin.listeners

/**
 * Created by Dani on 11.02.2018.
 */
interface ActionListener<in T> {
    fun onAdd(addedObject : T)
    fun onRemove(removedItemPosition : Int)
}