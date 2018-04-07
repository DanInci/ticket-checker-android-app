package ticket.checker.dialogs

import android.content.DialogInterface
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.AppTicketChecker.Companion.pretendedUserType
import ticket.checker.R
import ticket.checker.beans.Ticket
import ticket.checker.extras.BirthDateFormatException
import ticket.checker.extras.BirthDateIncorrectException
import ticket.checker.extras.UserType
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.ERROR_TICKET_EXISTS
import ticket.checker.extras.Util.ERROR_TICKET_VALIDATION
import ticket.checker.listeners.IScanDialogListener
import ticket.checker.services.ServiceManager
import java.util.*

/**
 * Created by Dani on 31.01.2018.
 */
class DialogScan : DialogFragment(), View.OnClickListener {
    var scanDialogListener: IScanDialogListener? = null

    private var ticketNumber: String? = null
    private var isTicketValidated : Boolean = false
    private var birthDate : Date? = null

    private var tvTicketNumber: TextView? = null
    private var tvOwnerName : TextView? = null
    private var tvOwnerBirthDate : TextView? = null
    private var viewValidateTicket : LinearLayout? = null
    private var viewSellTicket : LinearLayout? = null
    private var viewDeleteTicket : LinearLayout? = null

    private var loadingSpinner: ProgressBar? = null
    private var tvDescription: TextView? = null
    private var btnClose: ImageButton? = null
    private var etSoldTo: EditText? = null
    private var etSoldToBirthDate: EditText? = null
    private var btnAdd: Button? = null
    private var btnValidate: Button? = null
    private var btnDelete: Button? = null

    private val ticketCallback = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            if(response.isSuccessful) {
                switchViews(response.body())
            }
            else {
                if(response.code() == 404) {
                    switchViews(null)
                }
                else {
                    onErrorResponse(call, response)
                }
            }
        }
        override fun onFailure(call: Call<Ticket>, t: Throwable) {
            errorResult("Error connecting to the server!")
        }
    }
    private val verificationCallback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            if (response.isSuccessful) {
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable) {
            errorResult("Error connecting to the server!")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            ticketNumber = arguments?.getString(TICKET_NUMBER) ?: "NONE"
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_scan, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        scanDialogListener?.dismiss()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTicketNumber = view.findViewById(R.id.tvTicketNumber)
        tvTicketNumber?.text = ticketNumber
        tvOwnerName = view.findViewById(R.id.tvOwnerName)
        tvOwnerBirthDate = view.findViewById(R.id.tvOwnerBirthDate)
        viewValidateTicket = view.findViewById(R.id.viewValidateTicket)
        viewSellTicket = view.findViewById(R.id.viewSellTicket)
        viewDeleteTicket = view.findViewById(R.id.viewDeleteTicket)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvDescription = view.findViewById(R.id.tvDescription)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        etSoldTo = view.findViewById(R.id.etSoldTo)
        etSoldToBirthDate = view.findViewById(R.id.etSoldToBirthDate)
        btnAdd = view.findViewById(R.id.btnAdd)
        btnAdd?.setOnClickListener(this)
        btnValidate = view.findViewById(R.id.btnValidate)
        btnValidate?.setOnClickListener(this)
        btnDelete = view.findViewById(R.id.btnDelete)
        btnDelete?.setOnClickListener(this)

        requestTicketInfo()
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
                scanDialogListener?.dismiss()
            }
            R.id.btnValidate -> {
                showLoading()
                val call = ServiceManager.getTicketService().validateTicket(!isTicketValidated, ticketNumber as String)
                call.enqueue(verificationCallback as Callback<Ticket>)
            }
            R.id.btnAdd -> {
                if(validatedAdd()) {
                    showLoading()
                    val soldTo = etSoldTo?.text?.toString() ?: ""
                    val ticket = Ticket(ticketNumber as String, soldTo, birthDate)
                    val call = ServiceManager.getTicketService().createTicket(ticket)
                    call.enqueue(verificationCallback as Callback<Ticket>)
                }
            }
            R.id.btnDelete -> {
                showLoading()
                val call = ServiceManager.getTicketService().deleteTicketById(ticketNumber as String)
                call.enqueue(verificationCallback as Callback<Void>)
            }
        }
    }

    private fun requestTicketInfo() {
        tvOwnerName?.visibility = View.GONE
        tvOwnerBirthDate?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE
        tvDescription?.visibility = View.GONE
        viewValidateTicket?.visibility = View.GONE
        viewSellTicket?.visibility = View.GONE
        viewDeleteTicket?.visibility = View.GONE

        if(Util.isTicketFormatValid(ticketNumber)) {
            val call = ServiceManager.getTicketService().getTicketById(ticketNumber as String)
            call.enqueue(ticketCallback)
        }
        else {
            errorResult("Ticket id format is invalid!")
        }
    }

    private fun switchViews(ticket : Ticket?) {
        loadingSpinner?.visibility = View.GONE
        tvDescription?.visibility = View.VISIBLE

        if(ticket != null) {
            viewSellTicket?.visibility = View.GONE
            tvOwnerName?.visibility = if (ticket.soldTo.isNullOrEmpty()) View.GONE else View.VISIBLE
            tvOwnerBirthDate?.visibility = if (ticket.soldToBirthdate == null) View.GONE else View.VISIBLE
            tvOwnerName?.text = if (ticket.soldTo.isNullOrEmpty()) "~not specified~" else ticket.soldTo
            tvOwnerBirthDate?.text = if (ticket.soldToBirthdate == null) "~not specified~" else DATE_FORMAT.format(ticket.soldToBirthdate)

            when(pretendedUserType) {
                UserType.ADMIN -> {
                    viewDeleteTicket?.visibility = View.VISIBLE
                    viewValidateTicket?.visibility = View.VISIBLE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is validated!")
                        btnValidate?.text = "Invalidate Ticket"
                        btnValidate?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
                        isTicketValidated = true
                    }
                    else {
                        btnValidate?.text = "Validate Ticket"
                        btnValidate?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white, 0)
                        isTicketValidated = false
                    }
                }
                UserType.PUBLISHER -> {
                    viewDeleteTicket?.visibility = View.GONE
                    viewValidateTicket?.visibility = View.GONE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is validated!")
                    }
                    else {
                        errorResult("This ticket hasn't been validated!")
                    }
                }
                UserType.VALIDATOR -> {
                    viewDeleteTicket?.visibility = View.GONE
                    viewValidateTicket?.visibility = View.VISIBLE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is validated!")
                        btnValidate?.text = "Invalidate Ticket"
                        btnValidate?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white, 0)
                        isTicketValidated = true
                    }
                    else {
                        btnValidate?.text = "Validate Ticket"
                        btnValidate?.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white, 0)
                        isTicketValidated = false
                    }
                }
                UserType.USER -> {
                    viewDeleteTicket?.visibility = View.GONE
                    viewValidateTicket?.visibility = View.GONE
                    if(ticket.validatedAt != null) {
                        errorResult("This ticket is validated!")
                    }
                    else {
                        errorResult("This ticket hasn't been validated!")
                    }
                }
            }
        }
        else {
            tvOwnerName?.visibility = View.GONE
            tvOwnerBirthDate?.visibility = View.GONE
            viewValidateTicket?.visibility = View.GONE
            viewDeleteTicket?.visibility = View.GONE

            when(pretendedUserType) {
                UserType.ADMIN -> {
                    viewSellTicket?.visibility = View.VISIBLE
                }
                UserType.PUBLISHER -> {
                    viewSellTicket?.visibility = View.VISIBLE
                }
                UserType.VALIDATOR -> {
                    viewSellTicket?.visibility = View.GONE
                    errorResult("A ticket with this id was not found!")
                }
                UserType.USER -> {
                    viewSellTicket?.visibility = View.GONE
                    errorResult("A ticket with this id was not found!")
                }
            }
        }
    }

    private fun validatedAdd() : Boolean {
        var isValid = true
        val birthDateString = etSoldToBirthDate?.text.toString()
        if(!birthDateString.isEmpty()) {
            val soldTo = etSoldTo?.text.toString()
            if(soldTo.isEmpty()) {
                etSoldTo?.error = "You forgot the name"
                isValid = false
            }

            try {
                birthDate = Util.getBirthdateFromText(birthDateString)
            }
            catch(e :  BirthDateIncorrectException) {
                etSoldToBirthDate?.error = "The birth date cannot be in the future"
                isValid = false
            }
            catch(e : BirthDateFormatException) {
                etSoldToBirthDate?.error =  "Not valid date format. (dd.mm.yyyy)"
                isValid = false
            }
        }
        return isValid
    }

    private fun showLoading() {
        tvDescription?.visibility = View.GONE
        viewValidateTicket?.visibility = View.GONE
        viewSellTicket?.visibility = View.GONE
        viewDeleteTicket?.visibility = View.GONE
        btnClose?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager)
        if (!wasHandled) {
            when (response?.code()) {
                400 -> {
                    val error = Util.convertError(response.errorBody())
                    when (error.message) {
                        ERROR_TICKET_EXISTS -> {
                            errorResult("This ticket has already been added!")
                        }
                        ERROR_TICKET_VALIDATION -> {
                            val validated = if(isTicketValidated) "invalidated" else "validated"
                            errorResult("This ticket has already been $validated!")
                        }
                        else -> {
                            errorResult("Unexpected ticket id format!")
                        }
                    }
                }
                404 -> {
                    errorResult("It looks like the ticket doesn't exist anymore!")
                }
            }
        } else {
            dismiss()
        }
    }

    private fun errorResult(errorMsg: String) {
        btnClose?.visibility = View.VISIBLE
        loadingSpinner?.visibility = View.GONE
        tvDescription?.text = errorMsg
        tvDescription?.setTextColor(ContextCompat.getColor(context!!, R.color.materialYellow))
        tvDescription?.visibility = View.VISIBLE
    }

    companion object {
        private const val TICKET_NUMBER = "ticketNumber"

        fun newInstance(ticketNumber: String): DialogScan {
            val fragment = DialogScan()
            val args = Bundle()
            args.putString(TICKET_NUMBER, ticketNumber)
            fragment.arguments = args
            return fragment
        }
    }
}