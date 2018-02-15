package ticket.checker.admin.tickets

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.R
import ticket.checker.admin.listeners.ActionListener
import ticket.checker.beans.Ticket
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.services.ServiceManager

class DialogAddTicket : DialogFragment(), View.OnClickListener {
    var actionListener: ActionListener<Ticket>? = null

    private var btnClose : ImageButton? = null
    private var tvTitle : TextView? = null
    private var etTicketNumber : EditText? = null
    private var etSoldTo : EditText? = null
    private var submitButton : Button? = null
    private var loadingSpinner : ProgressBar? = null
    private var tvResult : TextView? = null

    private val submitCallback : Callback<Ticket> = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            loadingSpinner?.visibility = View.GONE
            submitButton?.visibility = View.VISIBLE
            if(response.isSuccessful) {
                actionListener?.onAdd(response.body() as Ticket)
                tvResult?.visibility = View.VISIBLE
                tvResult?.setTextColor(context.resources.getColor(R.color.yesGreen))
                tvResult?.text = "You have created the ticket successfully"
                etTicketNumber?.setText("")
                etTicketNumber?.requestFocus()
                etTicketNumber?.error = null
                etSoldTo?.setText("")
            }
            else {
                when(response.code()) {
                    400 -> {
                        etTicketNumber?.error = "This ticket id already exists!"
                    }
                    401 -> {
                        dismiss()
                        val authDialog = DialogInfo.newInstance("Session expired","You need to provide your authentication once again!", DialogType.AUTH_ERROR)
                        authDialog.isCancelable = false
                        authDialog.show(fragmentManager,"DIALOG_AUTH_ERROR")
                    }
                    403 -> {
                        val permissionDialog = DialogInfo.newInstance("Add failed","You don't have permissions to create tickets!", DialogType.ERROR)
                        permissionDialog.show(fragmentManager,"DIALOG_FAIL")
                    }
                    else -> {
                        val unknownError =  DialogInfo.newInstance("Add failed","There was an unexpected error!", DialogType.ERROR)
                        unknownError.show(fragmentManager, "DIALOG_FAIL")
                    }
                }
            }
        }

        override fun onFailure(call: Call<Ticket>?, t: Throwable?) {
            loadingSpinner?.visibility = View.GONE
            submitButton?.visibility = View.VISIBLE
            val dialogConnection = DialogInfo.newInstance("Connection error","There was an error connecting to the server", DialogType.ERROR)
            dialogConnection.showHeader(false)
            dialogConnection.show(fragmentManager,"DIALOG_FAIL")
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.dialog_add_ticket, container, false)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        tvTitle = view.findViewById(R.id.tvTitle)
        etTicketNumber = view.findViewById(R.id.etTicketNumber)
        etSoldTo = view.findViewById(R.id.etSoldTo)
        submitButton = view.findViewById(R.id.btnSubmit)
        submitButton?.setOnClickListener(this)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvResult = view.findViewById(R.id.tvResult)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View) {
       when(v.id) {
           R.id.btnClose -> {
               dismiss()
           }
           R.id.btnSubmit -> {
               if(validate()) {
                   submitTicket(etTicketNumber?.text.toString(), etSoldTo?.text.toString())
               }
           }
       }
    }

    private fun validate() : Boolean {
        val ticketNumber = etTicketNumber?.text.toString()
        return if(ticketNumber.isEmpty()) {
            etTicketNumber?.error = "This field is required"
            false
        }
        else {
            true
        }
    }

    private fun submitTicket(ticketNumber : String, soldTo : String) {
        tvResult?.visibility = View.INVISIBLE
        submitButton?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE

        val ticket = Ticket(ticketNumber, soldTo)
        val call = ServiceManager.getTicketService().createTicket(ticket)
        call.enqueue(submitCallback)
    }

}