//package ticket.checker.admin.tickets
//
//import android.graphics.Color
//import android.graphics.drawable.ColorDrawable
//import android.os.Bundle
//import androidx.fragment.app.DialogFragment
//import androidx.core.content.ContextCompat
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.view.Window
//import android.widget.*
//import retrofit2.Call
//import retrofit2.Callback
//import retrofit2.Response
//import ticket.checker.AppTicketChecker
//import ticket.checker.R
//import ticket.checker.admin.listeners.EditListener
//import ticket.checker.beans.Ticket
//import ticket.checker.beans.TicketUpdateDefinition
//import ticket.checker.extras.Util
//import ticket.checker.services.ServiceManager
//import java.time.LocalDate
//import java.time.format.DateTimeParseException
//import java.util.*
//
//class DialogEditTicket : DialogFragment(), View.OnClickListener {
//    var editListener: EditListener<Ticket>? = null
//
//    private lateinit var organizationId: UUID
//    private lateinit var ticketNumber : String
//
//    private lateinit var soldToBirthday : LocalDate
//
//    private lateinit var btnClose: ImageButton
//    private lateinit var tvTitle: TextView
//    private lateinit var etSoldTo: EditText
//    private lateinit var etSoldToBirthDate: EditText
//    private lateinit var etSoldToTelephone: EditText
//    private lateinit var bottomContainer : LinearLayout
//    private lateinit var editButton: Button
//    private lateinit var loadingSpinner: ProgressBar
//    private lateinit var tvResult: TextView
//
//    private val getCallback : Callback<Ticket> = object : Callback<Ticket> {
//        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
//            if (response.isSuccessful) {
//                loadingSpinner.visibility = View.GONE
//                editButton.visibility = View.VISIBLE
//                updateWithTicketInfo(response.body() as Ticket)
//            } else {
//                onErrorResponse(call, response)
//            }
//        }
//        override fun onFailure(call: Call<Ticket>, t: Throwable) {
//            loadingSpinner.visibility = View.GONE
//            onErrorResponse(call, null)
//        }
//    }
//
//    private val editCallback: Callback<Ticket> = object : Callback<Ticket> {
//        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
//            loadingSpinner.visibility = View.GONE
//            editButton.visibility = View.VISIBLE
//            if (response.isSuccessful) {
//                editListener?.onEdit(response.body() as Ticket)
//                dismiss()
//            } else {
//                onErrorResponse(call, response)
//            }
//        }
//        override fun onFailure(call: Call<Ticket>, t: Throwable) {
//            loadingSpinner.visibility = View.GONE
//            editButton.visibility = View.VISIBLE
//            onErrorResponse(call, null)
//        }
//    }
//
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        if (arguments != null) {
//            organizationId = arguments?.getSerializable(ORGANIZATION_ID) as UUID
//            ticketNumber = arguments?.getString(TICKET_NUMBER)!!
//        }
//    }
//
//    override fun onStart() {
//        super.onStart()
//        val getCall = ServiceManager.getTicketService().getTicketById(AppTicketChecker.selectedOrganization!!.id, ticketNumber)
//        getCall.enqueue(getCallback)
//    }
//
//    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
//                              savedInstanceState: Bundle?): View? {
//        val view = inflater.inflate(R.layout.dialog_edit_ticket, container, false)
//        btnClose = view.findViewById(R.id.btnClose)
//        btnClose.setOnClickListener(this)
//        tvTitle = view.findViewById(R.id.dialogTitle)
//        tvTitle.text = "Edit #$ticketNumber"
//        etSoldTo = view.findViewById(R.id.etSoldTo)
//        etSoldToBirthDate = view.findViewById(R.id.etSoldToBirthDate)
//        etSoldToTelephone = view.findViewById(R.id.etSoldToTelephone)
//        bottomContainer = view.findViewById(R.id.bottomContainer)
//        editButton = view.findViewById(R.id.btnEdit)
//        editButton.setOnClickListener(this)
//        loadingSpinner = view.findViewById(R.id.loadingSpinner)
//        tvResult = view.findViewById(R.id.tvResult)
//        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
//        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
//        dialog?.setCanceledOnTouchOutside(false)
//        return view
//    }
//
//    override fun onClick(v: View) {
//        when (v.id) {
//            R.id.btnClose -> {
//                dismiss()
//            }
//            R.id.btnEdit -> {
//                if (validate()) {
//                    editTicket(ticketNumber, etSoldTo.text.toString(), soldToBirthday, etSoldToTelephone.text.toString())
//                }
//            }
//        }
//    }
//
//    private fun updateWithTicketInfo(ticket : Ticket) {
//        etSoldTo.isEnabled = true
//        etSoldTo.setText(ticket.soldTo)
//        etSoldTo.error = null
//        etSoldTo.post { etSoldTo.setSelection(ticket.soldTo?.length ?: 0) }
//        etSoldToBirthDate.isEnabled = true
//        etSoldToBirthDate.setText(if(ticket.soldToBirthDay != null)  Util.DATE_FORMAT.format(ticket.soldToBirthDay) else "")
//        etSoldToBirthDate.error = null
//        etSoldToTelephone.isEnabled = true
//        etSoldToTelephone.setText(ticket.soldToTelephone)
//        etSoldToTelephone.error = null
//    }
//
//    private fun validate(): Boolean {
//        var isValid = true
//        tvResult.visibility = View.INVISIBLE
//
//        val soldTo = etSoldTo.text.toString()
//        if(soldTo.isEmpty()) {
//            etSoldTo.error = "You forgot the name"
//            isValid = false
//        }
//
//        val birthDayString = etSoldToBirthDate.text.toString()
//        if(birthDayString.isNotEmpty()) {
//            try {
//                this.soldToBirthday = LocalDate.parse(birthDayString, Util.DATE_FORMAT)
//            }
//            catch(e : DateTimeParseException) {
//                etSoldToBirthDate.error =  "Not valid date format. (dd.mm.yyyy)"
//                isValid = false
//            }
//        }
//        val now = LocalDate.now()
//        if(this.soldToBirthday.isAfter(now)) {
//            etSoldToBirthDate.error = "The birth date cannot be in the future"
//            isValid = false
//        }
//        return isValid
//    }
//
//    private fun editTicket(ticketNumber : String, soldTo: String, soldToBirthday : LocalDate?, etSoldToTelephone: String?) {
//        tvResult.visibility = View.INVISIBLE
//        editButton.visibility = View.GONE
//        loadingSpinner.visibility = View.VISIBLE
//
//        val definition = TicketUpdateDefinition(soldTo, soldToBirthday, etSoldToTelephone)
//        val call = ServiceManager.getTicketService().updateTicketById(organizationId, ticketNumber, definition)
//        call.enqueue(editCallback)
//    }
//
//    private fun onErrorResponse(call: Call<Ticket>, response: Response<Ticket>?) {
//        val wasHandled = Util.treatBasicError(call, response, fragmentManager!!)
//        if (!wasHandled) {
//            if (response?.code() == 404) {
//                bottomContainer.visibility = View.GONE
//                tvResult.visibility = View.VISIBLE
//                tvResult.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
//                tvResult.text = "A ticket with this id was not found!"
//            }
//        }
//    }
//
//    companion object {
//        private const val ORGANIZATION_ID = "organizationId"
//        private const val TICKET_NUMBER = "ticketNumber"
//
//        fun newInstance(organizationId: UUID, ticketNumber: String): DialogEditTicket {
//            val fragment = DialogEditTicket()
//            val args = Bundle()
//            args.putSerializable(ORGANIZATION_ID, organizationId)
//            args.putString(TICKET_NUMBER, ticketNumber)
//            fragment.arguments = args
//            return fragment
//        }
//    }
//
//}