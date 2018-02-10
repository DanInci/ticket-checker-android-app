package ticket.checker.admin.users

import android.os.Bundle
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.User
import ticket.checker.services.ServiceManager

class UsersFragment : AAdminFragment<User, Int>() {

    override fun setupItemsAdapter(): AItemsAdapter<User, Int> {
        return UsersAdapter(activity.applicationContext)
    }

    override fun loadHeader(filter: String) {
        val modifiedFilters = if (filter == "all") null else filter
        val call = ServiceManager.getNumbersService().getUserNumbers(modifiedFilters)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filter: String) {
        val modifiedFilters = if (filter == "all") null else filter
        val call = ServiceManager.getUserService().getUsers(modifiedFilters, page, LOAD_LIMIT)
        call.enqueue(itemsCallback)
    }

    companion object {
        private const val LOAD_LIMIT = 20

        fun newInstance(usersFilter : String): UsersFragment {
            val fragment = UsersFragment()
            val args = Bundle()
            args.putString(FILTER, usersFilter)
            fragment.arguments = args
            return fragment
        }
    }
}
