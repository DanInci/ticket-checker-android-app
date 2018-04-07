package ticket.checker.admin.tickets

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.FILTER_SEARCH
import ticket.checker.ActivityControlPanel.Companion.FILTER_VALIDATED
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.Ticket
import ticket.checker.services.ServiceManager

class TicketsFragment : AAdminFragment<Ticket, Int>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapter<Ticket, Int> {
       return TicketsAdapter(context!!)
    }

    override fun loadHeader(filterType : String?, filterValue : String) {
        val call = ServiceManager.getStatisticsService().getTicketNumbers(filterType,filterValue)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filterType: String?, filterValue : String?) {
        val call = ServiceManager.getTicketService().getTickets(filterType, filterValue, page, loadLimit)
        call.enqueue(itemsCallback)
    }

    override fun onAdd(addedObject: Ticket) {
        when(filterType) {
            null -> {
                itemsAdapter.itemAdded(addedObject)
            }
            FILTER_VALIDATED -> {
                if(filterValue != "true") {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
            FILTER_SEARCH -> {
                if(addedObject.ticketId.startsWith(filterValue, true) || (addedObject.soldTo != null && addedObject.soldTo.startsWith(filterValue, true))) {
                    itemsAdapter.itemAdded(addedObject)
                }
            }
        }
    }

    override fun onEdit(editedObject: Ticket, editedObjectPosition: Int) {
        when(filterType) {
            null -> {
                itemsAdapter.itemEdited(editedObject, editedObjectPosition)
            }
            FILTER_VALIDATED -> {
                if((editedObject.validatedAt == null && filterValue == "false") || (editedObject.validatedAt != null && filterValue == "true")) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
            FILTER_SEARCH -> {
                if((editedObject.soldTo != null && editedObject.soldTo.startsWith(filterValue, true)) || editedObject.ticketId.startsWith(filterValue, true)) {
                    itemsAdapter.itemEdited(editedObject, editedObjectPosition)
                }
                else {
                    itemsAdapter.itemRemoved(editedObjectPosition)
                }
            }
        }
    }

    companion object {
        fun newInstance(filterType : String?, filterValue : String): TicketsFragment {
            val fragment = TicketsFragment()
            val args = Bundle()
            args.putString(FILTER_TYPE, filterType)
            args.putString(FILTER_VALUE, filterValue)
            fragment.arguments = args
            return fragment
        }
    }
}