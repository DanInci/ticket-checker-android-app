package ticket.checker.admin.members

import android.content.Intent
import android.os.Bundle
import android.view.View
import ticket.checker.ActivityControlPanel
import ticket.checker.ActivityControlPanel.Companion.FILTER_ROLE
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.ActivityMenu.Companion.ORGANIZATION_ID
import ticket.checker.R
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapterWithHeader
import ticket.checker.admin.tickets.ActivityTicketDetails
import ticket.checker.beans.OrganizationMember
import ticket.checker.beans.OrganizationMemberList
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class OrganizationMembersFragment : AAdminFragment<OrganizationMember, OrganizationMemberList, Int>() {

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
            FILTER_ROLE -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, Util.PAGE_SIZE, if(filterValue != null) OrganizationRole.from(filterValue) else null, null)
            FILTER_SEARCH -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, Util.PAGE_SIZE, null, filterValue)
            else -> ServiceManager.getOrganizationService().getOrganizationMembers(organizationId, page, Util.PAGE_SIZE, null, null)
        }
        call.enqueue(itemsCallback)
    }

    override fun onItemClick(view: View, position: Int) {
        val item = itemsAdapter.getItemByPosition(position)
        if(item != null) {
            val intent = Intent(context, ActivityOrganizationMemberDetails::class.java)
            intent.putExtra(Util.POSITION, position)
            intent.putExtra(ActivityOrganizationMemberDetails.ORGANIZATION_ID, item.organizationId)
            intent.putExtra(ActivityOrganizationMemberDetails.USER_ID, item.userId)
            activity!!.startActivityForResult(intent, ActivityControlPanel.CHANGES_TO_ADAPTER_ITEM)
            activity!!.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun onAdd(addedObject: OrganizationMember) {
        super.onAdd(addedObject)
        when(filterType) {
            null, "" -> {
                itemsAdapter.itemAdded(addedObject.toOrganizationMemberList())
            }
            FILTER_ROLE -> {
                if(addedObject.role.role == filterValue) {
                    itemsAdapter.itemAdded(addedObject.toOrganizationMemberList())
                }
            }
            FILTER_SEARCH -> {
                if(addedObject.name.startsWith(filterValue, true)) {
                    itemsAdapter.itemAdded(addedObject.toOrganizationMemberList())
                }
            }
        }
    }

    override fun onEdit(editedObject: OrganizationMember, editedObjectPosition: Int) {
        when(filterType) {
            null, "" -> {
                itemsAdapter.itemEdited(editedObject.toOrganizationMemberList(), editedObjectPosition)
            }
            FILTER_ROLE -> {
                if(editedObject.role.role == filterValue) {
                    itemsAdapter.itemEdited(editedObject.toOrganizationMemberList(), editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
            FILTER_SEARCH -> {
                if(editedObject.name.startsWith(filterValue, true)) {
                    itemsAdapter.itemEdited(editedObject.toOrganizationMemberList(), editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
        }
    }

    override fun getEmptyText(): String {
        return when(filterType) {
            FILTER_ROLE -> {
               "No ${filterValue}s"
            }
            else -> {
                "No members"
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
