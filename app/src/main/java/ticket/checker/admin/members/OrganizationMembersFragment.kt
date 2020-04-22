package ticket.checker.admin.members

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.FILTER_ROLE
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.ActivityMenu.Companion.ORGANIZATION_ID
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.beans.OrganizationMemberList
import ticket.checker.extras.OrganizationRole
import ticket.checker.services.ServiceManager
import java.util.*

class OrganizationMembersFragment : AAdminFragment<OrganizationMemberList, Int>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapterWithHeader<OrganizationMemberList, Int> {
        val usersAdapter = OrganizationMembersAdapter(context!!)
        usersAdapter.setHasStableIds(true)
        return usersAdapter
    }

    override fun loadHeader(filterType: String?, filterValue : String) {
        val call = when(filterType) {
            FILTER_ROLE -> ServiceManager.getStatisticsService().getOrganizationMembersNumber(organizationId, OrganizationRole.from(filterValue), null)
            FILTER_SEARCH -> ServiceManager.getStatisticsService().getOrganizationMembersNumber(organizationId, null, filterValue)
            else -> ServiceManager.getStatisticsService().getOrganizationMembersNumber(organizationId, null, null)
        }
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filterType: String?, filterValue : String?) {
        val call = when(filterType) {
            FILTER_ROLE -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, loadLimit, if(filterValue != null) OrganizationRole.from(filterValue) else null, null)
            FILTER_SEARCH -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, loadLimit, null, filterValue)
            else -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, loadLimit, null, null)
        }
        call.enqueue(itemsCallback)
    }

    override fun onAdd(addedObject: OrganizationMemberList) {
        when(filterType) {
            null -> {
                itemsAdapter.itemAdded(addedObject)
            }
            FILTER_ROLE -> {
                if(addedObject.role.role == filterValue) {
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

    override fun onEdit(editedObject: OrganizationMemberList, editedObjectPosition: Int) {
        when(filterType) {
            null -> {
                itemsAdapter.itemEdited(editedObject, editedObjectPosition)
            }
            FILTER_ROLE -> {
                if(editedObject.role.role == filterValue) {
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
        fun newInstance(organizationId: UUID, filterType : String?, filterValue : String): OrganizationMembersFragment {
            val fragment = OrganizationMembersFragment()
            val args = Bundle()
            args.putSerializable(ORGANIZATION_ID, organizationId)
            args.putString(FILTER_TYPE, filterType)
            args.putString(FILTER_VALUE, filterValue)
            fragment.arguments = args
            return fragment
        }
    }
}
