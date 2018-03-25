package ticket.checker.admin.tickets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import ticket.checker.ActivityControlPanel.Companion.CHANGES_TO_ADAPTER_ITEM
import ticket.checker.ActivityControlPanel.Companion.LIST_ALL
import ticket.checker.ActivityControlPanel.Companion.LIST_NOT_VALIDATED
import ticket.checker.ActivityControlPanel.Companion.LIST_VALIDATED
import ticket.checker.R
import ticket.checker.admin.AItemsAdapter
import ticket.checker.beans.Ticket
import ticket.checker.extras.Util
import ticket.checker.extras.Util.CURRENT_TICKET
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.POSITION
import java.util.*

/**
 * Created by Dani on 08.02.2018.
 */
class TicketsAdapter(val context: Context) : AItemsAdapter<Ticket,Array<Int>>(context) {

    override fun inflateItemHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.ticket_row, parent, false)
        return TicketHolder(view)
    }

    override fun inflateHeaderHolder(parent: ViewGroup): RecyclerView.ViewHolder {
        val view =  inflater.inflate(R.layout.ticket_row_header, parent, false)
        return HeaderHolder(view)
    }

    override fun updateItemInfo(holder: RecyclerView.ViewHolder, item: Ticket) {
        (holder as TicketHolder).updateTicketHolderInfo(item)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filter: String, itemStats: Array<Int>?) {
        (holder as HeaderHolder).updateTicketHeaderInfo(filter, itemStats)
    }

    override fun launchInfoActivity(view: View, position : Int) {
        val activity = context as Activity
        val intent  = Intent(activity, ActivityTicketDetails::class.java)
        intent.putExtra(POSITION, position)
        intent.putExtra(CURRENT_TICKET, items[position-1])
        activity.startActivityForResult(intent, CHANGES_TO_ADAPTER_ITEM)
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    override fun itemAdded(addedItem: Ticket) {
        val newItemStats = itemStats?: arrayOf(0,0)
        newItemStats[0]++
        items.add(0, addedItem)
        notifyItemInserted(1)
        updateHeaderInfo(filter, newItemStats)
    }

    override fun itemEdited(editedItem: Ticket, position: Int) {
        if(isItemPosition(position)) {
            val oldObject = items[position-1]
            val changedValidation = editedItem.validatedAt != oldObject.validatedAt
            if(changedValidation) {
                val newItemStats = itemStats ?: arrayOf(0, 0)
                if(editedItem.validatedAt != null) {
                    newItemStats[1]++
                }
                else {
                    newItemStats[1]--
                }

                if(filter == LIST_ALL) {  // the validation change would only be visible if all kind of tickets are shown
                    items.removeAt(position -1)
                    items.add(position - 1, editedItem)
                    notifyItemChanged(position)
                    updateHeaderInfo(filter, newItemStats)
                }
                else {
                    items.removeAt(position - 1)
                    notifyItemRemoved(position)
                    updateHeaderInfo(filter, newItemStats)
                }
            }
            else {
                items.removeAt(position -1)
                items.add(position - 1, editedItem)
                notifyItemChanged(position)
            }
        }
    }

    override fun itemRemoved(position: Int) {
        if(isItemPosition(position)) {
            val newItemStats = itemStats ?: arrayOf(0, 0)
            newItemStats[0]--
            if (items[position - 1].validatedAt != null) {
                newItemStats[1]--
            }
            items.removeAt(position - 1)
            notifyItemRemoved(position)
            updateHeaderInfo(filter, newItemStats)
        }
    }

    private class TicketHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val leftBar = itemView.findViewById<View>(R.id.leftBar)
        private val tvTicketId = itemView.findViewById<TextView>(R.id.tvTicketId)
        private val tvSoldAtText = itemView.findViewById<TextView>(R.id.tvSoldAtText)
        private val tvTicketSoldAt = itemView.findViewById<TextView>(R.id.tvTicketSoldAt)
        private val tvValidated = itemView.findViewById<TextView>(R.id.tvValidated)

        fun updateTicketHolderInfo(ticket: Ticket) {
            setTicketId(ticket.ticketId)
            setValidated(ticket.validatedAt)
            setSoldAt(ticket.soldAt)
        }

        private fun setTicketId(ticketId: String) {
            tvTicketId.text = "#$ticketId"
        }

        private fun setValidated(date: Date?) {
            if (date != null) {
                tvValidated.visibility = View.VISIBLE
                tvValidated.text = Util.formatDate(date)
                leftBar.setBackgroundColor(itemView.resources.getColor(R.color.yesGreen))
            } else {
                tvValidated.visibility = View.GONE
                leftBar.setBackgroundColor(itemView.resources.getColor(R.color.darkerGrey))
            }
        }

        private fun setSoldAt(soldAt: Date?) {
            if (soldAt == null) {
                tvSoldAtText.visibility = View.GONE
                tvTicketSoldAt.visibility = View.GONE
            } else {
                tvSoldAtText.visibility = View.VISIBLE
                tvTicketSoldAt.visibility = View.VISIBLE
                tvTicketSoldAt.text = DATE_FORMAT.format(soldAt)
            }
        }
    }

    private class HeaderHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvTotalTickets = itemView.findViewById<TextView>(R.id.ticketNumbers)
        private val tvValidatedTickets = itemView.findViewById<TextView>(R.id.ticketsValidatedNumbers)

        fun updateTicketHeaderInfo(filter : String, ticketNumbers : Array<Int>?) {
            if(ticketNumbers == null || ticketNumbers.isEmpty()) {
                tvTotalTickets.visibility = View.INVISIBLE
                tvValidatedTickets.visibility = View.INVISIBLE
            }
            else {
                tvTotalTickets.visibility = View.VISIBLE
                val totalTickets = ticketNumbers.getOrElse(0, {0})
                val tickets = if (totalTickets == 1)  "ticket" else "tickets"
                tvTotalTickets.text = "There is a total of $totalTickets $tickets"
                when (filter) {
                    LIST_ALL -> {
                        tvValidatedTickets.visibility = View.VISIBLE
                        val validatedTickets = ticketNumbers.getOrElse(1, {0})
                        if (validatedTickets == 0) {
                            tvValidatedTickets.setTextColor(itemView.resources.getColor(R.color.darkerGrey))
                            tvValidatedTickets.text = "No ticket has been validated yet"
                        } else {
                            tvValidatedTickets.setTextColor(itemView.resources.getColor(R.color.yesGreen))
                            val are = if (validatedTickets == 1) "is" else "are"
                            tvValidatedTickets.text = "$validatedTickets of them $are validated"
                        }
                    }
                    LIST_VALIDATED, LIST_NOT_VALIDATED -> {
                        tvValidatedTickets.visibility = View.GONE
                    }
                }
            }
        }
    }
}