package ticket.checker.admin.tickets

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.support.v4.content.ContextCompat
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.TextView
import ticket.checker.ActivityControlPanel.Companion.CHANGES_TO_ADAPTER_ITEM
import ticket.checker.ActivityControlPanel.Companion.FILTER_VALIDATED
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
class TicketsAdapter(val context: Context) : AItemsAdapter<Ticket, Int>(context) {

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

    override fun setHeaderVisibility(holder: RecyclerView.ViewHolder, isVisible: Boolean) {
        (holder as HeaderHolder).setVisibility(isVisible)
    }

    override fun updateHeaderInfo(holder: RecyclerView.ViewHolder, filterType: String?, filterValue : String, itemStats: Int?) {
        (holder as HeaderHolder).updateTicketHeaderInfo(filterType, filterValue, itemStats)
    }

    override fun launchInfoActivity(view: View, position : Int) {
        if(isItemPosition(position)) {
            val activity = context as Activity
            val intent = Intent(activity, ActivityTicketDetails::class.java)
            intent.putExtra(POSITION, position)
            intent.putExtra(CURRENT_TICKET, items[position - 1])
            activity.startActivityForResult(intent, CHANGES_TO_ADAPTER_ITEM)
            activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun itemAdded(addedItem: Ticket) {
        var newItemStats = itemStats?: 0
        newItemStats++
        items.add(0, addedItem)
        notifyItemInserted(1)
        updateHeaderInfo(filterType, filterValue, newItemStats)
    }

    override fun itemEdited(editedItem: Ticket, position: Int) {
        if(isItemPosition(position)) {
            items.removeAt(position -1)
            items.add(position - 1, editedItem)
            notifyItemChanged(position)
        }
    }

    override fun itemRemoved(position: Int) {
        if(isItemPosition(position)) {
            var newItemStats = itemStats ?: 0
            newItemStats--
            if (items[position - 1].validatedAt != null) {
                newItemStats--
            }
            items.removeAt(position - 1)
            notifyItemRemoved(position)
            updateHeaderInfo(filterType, filterValue, newItemStats)
        }
    }

    private class TicketHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        private val leftBar = itemView.findViewById<View>(R.id.leftBar)
        private val tvTicketId = itemView.findViewById<TextView>(R.id.tvTicketId)
        private val tvValidated = itemView.findViewById<TextView>(R.id.tvValidated)
        private val tvSoldToText = itemView.findViewById<TextView>(R.id.tvSoldToText)
        private val tvTicketSoldTo = itemView.findViewById<TextView>(R.id.tvTicketSoldTo)
        private val tvSoldAtText = itemView.findViewById<TextView>(R.id.tvSoldAtText)
        private val tvTicketSoldAt = itemView.findViewById<TextView>(R.id.tvTicketSoldAt)


        fun updateTicketHolderInfo(ticket: Ticket) {
            setTicketId(ticket.ticketId)
            setValidated(ticket.validatedAt)
            setSoldTo(ticket.soldTo)
            setSoldAt(ticket.soldAt)
        }

        private fun setTicketId(ticketId: String) {
            tvTicketId.text = "#$ticketId"
        }

        private fun setValidated(date: Date?) {
            if (date != null) {
                tvValidated.visibility = View.VISIBLE
                if((itemView.context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.orientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    tvValidated.text = Util.formatDate(date, true)
                }
                else {
                    tvValidated.text = Util.formatDate(date, false)
                }
                leftBar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.yesGreen))
            } else {
                tvValidated.visibility = View.GONE
                leftBar.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.darkerGrey))
            }
        }

        private fun setSoldTo(soldTo: String?) {
            if(soldTo.isNullOrEmpty()) {
                tvSoldToText.text = "Sold"
                tvTicketSoldTo.visibility = View.GONE
            }
            else {
                tvSoldToText.text = "Sold to "
                tvTicketSoldTo.visibility = View.VISIBLE
                tvTicketSoldTo.text = soldTo
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

        fun setVisibility(isVisible : Boolean) {
            tvTotalTickets.visibility = if(isVisible) View.VISIBLE else View.INVISIBLE
        }

        fun updateTicketHeaderInfo(filterType : String?, filterValue : String, ticketNumbers : Int?) {
            if(ticketNumbers == null)
                return

            if(ticketNumbers == 0) {
                tvTotalTickets?.visibility = View.VISIBLE
                tvTotalTickets.text = "No tickets found"
            }
            else {
                tvTotalTickets?.visibility = View.VISIBLE
                val tickets = when(filterType) {
                    null -> {
                        if (ticketNumbers == 1) "ticket" else "tickets"
                    }
                    FILTER_VALIDATED -> {
                        if (ticketNumbers == 1) {
                            if (filterValue == "true") {
                                "validated ticket"
                            } else {
                                "not validated ticket"
                            }
                        } else {
                            if (filterValue == "true") {
                                "validated tickets"
                            } else {
                                "not validated tickets"
                            }
                        }
                    }
                    else -> {
                        if (ticketNumbers == 1) "ticket" else "tickets"

                    }
                }
                tvTotalTickets.text = "There is a total of $ticketNumbers $tickets"
            }
        }
    }
}