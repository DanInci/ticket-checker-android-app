package ticket.checker.dialogs

import android.app.Dialog
import android.content.DialogInterface
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
import ticket.checker.beans.ErrorResponse
import ticket.checker.beans.Ticket
import ticket.checker.extras.Util
import ticket.checker.extras.Util.ERROR_TICKET_EXISTS
import ticket.checker.extras.Util.ERROR_TICKET_VALIDATION
import ticket.checker.extras.Util.ROLE_ADMIN
import ticket.checker.listeners.IScanDialogListener
import ticket.checker.services.ServiceManager

/**
 * Created by Dani on 31.01.2018.
 */
class DialogScan : DialogFragment(), View.OnClickListener {
    var scanDialogListener: IScanDialogListener? = null

    private var ticketNumber: String? = null
    private var pretendedUserRole: String? = null

    private var tvTicketNumber: TextView? = null
    private var loadingSpinner: ProgressBar? = null
    private var tvDescription: TextView? = null
    private var btnClose: ImageButton? = null
    private var addAction: LinearLayout? = null
    private var etSoldTo: EditText? = null
    private var btnAdd: Button? = null
    private var btnValidate: Button? = null
    private var btnDelete: Button? = null

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
            ticketNumber = arguments.getString(TICKET_NUMBER)
            pretendedUserRole = arguments.getString(USER_ROLE)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater?.inflate(R.layout.dialog_scan, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onDismiss(dialog: DialogInterface?) {
        super.onDismiss(dialog)
        scanDialogListener?.dismiss()
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvTicketNumber = view?.findViewById(R.id.tvTicketNumber)
        loadingSpinner = view?.findViewById(R.id.loadingSpinner)
        loadingSpinner?.visibility = View.GONE
        tvDescription = view?.findViewById(R.id.tvDescription)
        btnClose = view?.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        addAction = view?.findViewById(R.id.addAction)
        etSoldTo = view?.findViewById(R.id.etSoldTo)
        btnAdd = view?.findViewById(R.id.btnAdd)
        btnAdd?.setOnClickListener(this)
        btnValidate = view?.findViewById(R.id.btnValidate)
        btnValidate?.setOnClickListener(this)
        btnDelete = view?.findViewById(R.id.btnDelete)
        btnDelete?.setOnClickListener(this)

        tvTicketNumber?.text = ticketNumber
        switchViews()
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.btnClose -> {
                dismiss()
                scanDialogListener?.dismiss()
            }
            R.id.btnValidate -> {
                showLoading()
                val call = ServiceManager.getTicketService().validateTicket(true, ticketNumber as String)
                call.enqueue(verificationCallback as Callback<Void>)
            }
            R.id.btnAdd -> {
                showLoading()
                val soldTo = etSoldTo?.text?.toString() ?: ""
                val ticket = Ticket(ticketNumber as String, soldTo)
                val call = ServiceManager.getTicketService().createTicket(ticket)
                call.enqueue(verificationCallback as Callback<Ticket>)
            }
            R.id.btnDelete -> {
                showLoading()
                val call = ServiceManager.getTicketService().deleteTicketById(ticketNumber as String)
                call.enqueue(verificationCallback as Callback<Void>)
            }
        }
    }

    private fun switchViews() {
        when (pretendedUserRole) {
            ROLE_ADMIN -> {
                btnValidate?.visibility = View.VISIBLE
                addAction?.visibility = View.VISIBLE
                btnDelete?.visibility = View.VISIBLE
            }
            else -> {
                btnValidate?.visibility = View.VISIBLE
                addAction?.visibility = View.GONE
                btnDelete?.visibility = View.GONE
            }
        }
    }

    private fun showLoading() {
        tvDescription?.visibility = View.GONE
        btnValidate?.visibility = View.GONE
        addAction?.visibility = View.GONE
        btnDelete?.visibility = View.GONE
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
                            errorResult("This ticket id already exists!")
                        }
                        ERROR_TICKET_VALIDATION -> {
                            errorResult("This ticket has already been validated!")
                        }
                        else -> {
                            errorResult("Unexpected ticket id format!")
                        }
                    }
                }
                404 -> {
                    errorResult("A ticket with this id was not found!")
                }
            }
        }
        else {
            dismiss()
        }
    }

    private fun errorResult(errorMsg: String) {
        btnClose?.visibility = View.VISIBLE
        loadingSpinner?.visibility = View.GONE
        tvDescription?.text = errorMsg
        tvDescription?.setTextColor(resources.getColor(R.color.materialYellow))
        tvDescription?.visibility = View.VISIBLE
    }

    companion object {
        private const val TICKET_NUMBER = "ticketNumber"
        private const val USER_ROLE = "userRole"

        fun newInstance(ticketNumber: String, userRole: String): DialogScan {
            val fragment = DialogScan()
            val args = Bundle()
            args.putString(TICKET_NUMBER, ticketNumber)
            args.putString(USER_ROLE, userRole)
            fragment.arguments = args
            return fragment
        }
    }
}