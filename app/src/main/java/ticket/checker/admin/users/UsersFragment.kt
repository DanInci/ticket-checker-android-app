package ticket.checker.admin.users

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.FILTER_ROLE
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.User
import ticket.checker.services.ServiceManager

class UsersFragment : AAdminFragment<User, Int>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapter<User, Int> {
        val usersAdapter = UsersAdapter(context!!)
        usersAdapter.setHasStableIds(true)
        return usersAdapter
    }

    override fun loadHeader(filterType: String?, filterValue : String) {
        val call = ServiceManager.getStatisticsService().getUserNumbers(filterType, filterValue)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filterType: String?, filterValue : String?) {
        val call = ServiceManager.getUserService().getUsers(filterType, filterValue, page, loadLimit)
        call.enqueue(itemsCallback)
    }

    override fun onAdd(addedObject: User) {
        when(filterType) {
            null -> {
                itemsAdapter.itemAdded(addedObject)
            }
            FILTER_ROLE -> {
                if(addedObject.role == "ROLE_" + filterValue.toUpperCase()) {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
            FILTER_SEARCH -> {
                if(addedObject.name.startsWith(filterValue, true)) {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
        }
    }

    override fun onEdit(editedObject: User, editedObjectPosition: Int) {
        when(filterType) {
            null -> {
                itemsAdapter.itemEdited(editedObject, editedObjectPosition)
            }
            FILTER_ROLE -> {
                if(editedObject.role == "ROLE_" + filterValue.toUpperCase()) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
            FILTER_SEARCH -> {
                if(editedObject.name.startsWith(filterValue, true)) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
        }
    }

    companion object {
        fun newInstance(filterType : String?, filterValue : String): UsersFragment {
            val fragment = UsersFragment()
            val args = Bundle()
            args.putString(FILTER_TYPE, filterType)
            args.putString(FILTER_VALUE, filterValue)
            fragment.arguments = args
            return fragment
        }
    }
}
