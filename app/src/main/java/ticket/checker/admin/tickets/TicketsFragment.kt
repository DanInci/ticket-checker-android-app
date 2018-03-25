package ticket.checker.admin.tickets

import android.os.Bundle
import ticket.checker.ActivityControlPanel.Companion.LIST_ALL
import ticket.checker.ActivityControlPanel.Companion.LIST_NOT_VALIDATED
import ticket.checker.ActivityControlPanel.Companion.LIST_VALIDATED
import ticket.checker.admin.AAdminFragment
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.Ticket
import ticket.checker.services.ServiceManager

class TicketsFragment : AAdminFragment<Ticket, Array<Int>>() {

    override val loadLimit: Int
        get() = 20

    override fun setupItemsAdapter(): AItemsAdapter<Ticket, Array<Int>> {
       return TicketsAdapter(context!!)
    }

    override fun loadHeader(filter : String) {
        val call = ServiceManager.getStatisticsService().getFilteredTicketNumbers(filter)
        call.enqueue(headerCallback)
    }

    override fun loadItems(page: Int, filter : String) {
        val ticketService = ServiceManager.getTicketService()
        when (filter) {
            LIST_ALL -> {
                val call = ticketService.getTickets(null, page, loadLimit)
                call.enqueue(itemsCallback)
            }
            LIST_VALIDATED -> {
                val call = ticketService.getTickets(true, page, loadLimit)
                call.enqueue(itemsCallback)
            }
            LIST_NOT_VALIDATED -> {
                val call = ticketService.getTickets(false, page, loadLimit)
                call.enqueue(itemsCallback)
            }
        }
    }

    override fun onAdd(addedObject: Ticket) {
        if(filter != LIST_VALIDATED) {  // you cant find the new ticket in the validated section
            itemsAdapter.itemAdded(addedObject)
        }
    }

    override fun onEdit(editedObject: Ticket, editedObjectPosition: Int) {
        itemsAdapter.itemEdited(editedObject, editedObjectPosition)
    }

    companion object {
        fun newInstance(ticketsFilter : String): TicketsFragment {
            val fragment = TicketsFragment()
            val args = Bundle()
            args.putString(FILTER, ticketsFilter)
            fragment.arguments = args
            return fragment
        }
    }
}