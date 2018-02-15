package ticket.checker.admin.tickets

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityAdmin.Companion.ITEM_REMOVED
import ticket.checker.ActivityAdmin.Companion.TICKET_CHANGE_VALIDATION
import ticket.checker.R
import ticket.checker.beans.Ticket
import ticket.checker.dialogs.DialogExitListener
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogResponseListener
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT_WITH_HOUR
import ticket.checker.extras.Util.POSITION
import ticket.checker.extras.Util.TICKET_NUMBER
import ticket.checker.extras.Util.TICKET_STATUS
import ticket.checker.extras.Util.userName
import ticket.checker.services.ServiceManager
import java.util.*

class ActivityTicketDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {

    private var ticketId: String = ""
    private var ticketPosition : Int = -1
    private var isValidated : Boolean = false
    private var itemWasRemoved : Boolean = false

    private val toolbar : Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvStatus : TextView by lazy {
        findViewById<TextView>(R.id.status)
    }
    private val tvSoldTo : TextView by lazy {
        findViewById<TextView>(R.id.soldTo)
    }
    private val tvSoldAt : TextView by lazy {
        findViewById<TextView>(R.id.soldAt)
    }
    private val tvSoldBy : TextView by lazy {
        findViewById<TextView>(R.id.soldBy)
    }
    private val tvValidatedAt : TextView by lazy {
        findViewById<TextView>(R.id.validatedAt)
    }
    private val tvValidatedBy : TextView by lazy {
        findViewById<TextView>(R.id.validatedBy)
    }
    private val btnValidate :  Button by lazy {
        findViewById<Button>(R.id.btnValidate)
    }
    private val btnRemove : Button by lazy {
        findViewById<Button>(R.id.btnRemove)
    }
    private val btnBack : ImageView by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }

    private val loadingSpinner : ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }

    private val validationCallback = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            if(response.isSuccessful) {
                isValidated = !isValidated
                updateViewsWithValidation()
                switchToLoadingView(false)
            }
            else {
                switchToLoadingView(false)
                when(response.code()) {
                    400 -> {
                        val error = Util.convertError(response.errorBody())
                        val dialogValidationError = DialogInfo.newInstance("Validation failed", error.message, DialogType.ERROR)
                        dialogValidationError.show(supportFragmentManager, "DIALOG_ERROR")
                    }
                    else -> {
                        onErrorResponse("Validation failed", response.code())
                    }
                }
            }
        }
        override fun onFailure(call: Call<Void>?, t: Throwable?) {
            switchToLoadingView(false)
            onErrorResponse("Validation failed", -1)
        }
    }
    private val removalCallback = object : Callback<Void> {
        override fun onResponse(call: Call<Void>, response: Response<Void>) {
            switchToLoadingView(false)
            if(response.isSuccessful) {
                val dialogRemoveSuccessful = DialogInfo.newInstance("Remove successful", "Ticket #$ticketId was sucessfully removed!", DialogType.SUCCESS)
                dialogRemoveSuccessful.dialogExitListener = this@ActivityTicketDetails
                dialogRemoveSuccessful.show(supportFragmentManager, "DIALOG_REMOVE_SUCCESSFUL")
            }
            else {
                onErrorResponse("Remove failed", response.code())
            }
        }
        override fun onFailure(call: Call<Void>?, t: Throwable?) {
            switchToLoadingView(false)
            onErrorResponse("Remove failed", -1)
        }
    }
    private val ticketInfoCallback = object : Callback<Ticket> {
        override fun onResponse(call: Call<Ticket>, response: Response<Ticket>) {
            if(response.isSuccessful) {
                updateTicketInfo(response.body() as Ticket)
            }
            else {
                onErrorResponse("Loading failed", response.code())
            }
        }
        override fun onFailure(call: Call<Ticket>?, t: Throwable?) {
            onErrorResponse("Loading failed", -1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ticketId = intent.getStringExtra(TICKET_NUMBER)
        ticketPosition = intent.getIntExtra(POSITION, -1)
        isValidated = savedInstanceState?.getBoolean(TICKET_STATUS) ?: intent.getBooleanExtra(TICKET_STATUS, false)
        setContentView(R.layout.activity_ticket_details)
        (findViewById<TextView>(R.id.toolbarTitle) as TextView).text = "Ticket #" + ticketId
        updateViewsWithValidation()
        setSupportActionBar(toolbar)
        btnValidate.setOnClickListener(this)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getTicketService().getTicketById(ticketId)
        call.enqueue(ticketInfoCallback)
    }

    override fun onBackPressed() {
        val data = Intent()
        data.putExtra(POSITION, ticketPosition)
        if(itemWasRemoved) {
            setResult(ITEM_REMOVED, data)
        }
        else {
            if(isValidated != intent.getBooleanExtra(TICKET_STATUS, false)) {
                setResult(TICKET_CHANGE_VALIDATION, data)
            }
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(TICKET_STATUS, isValidated)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnValidate -> {
                switchToLoadingView(true)
                val call = ServiceManager.getTicketService().validateTicket(!isValidated, ticketId)
                call.enqueue(validationCallback)
            }
            R.id.btnRemove -> {
                val dialogConfirm = DialogInfo.newInstance("Confirm removal", "Are you sure you want to remove ticket #" + ticketId, DialogType.YES_NO)
                dialogConfirm.dialogResponseListener = this
                dialogConfirm.show(supportFragmentManager, "DIALOG_CONFIRM_REMOVAL")
            }
        }
    }

    override fun onResponse(response: Boolean) {
        if(response) {
            switchToLoadingView(true)
            val call = ServiceManager.getTicketService().deleteTicketById(ticketId)
            call.enqueue(removalCallback)
        }
    }

    private fun switchToLoadingView(isLoading : Boolean) {
        loadingSpinner.visibility = if(isLoading) View.VISIBLE else View.GONE
        btnRemove.visibility = if(isLoading) View.GONE else View.VISIBLE
        btnValidate.visibility = if(isLoading) View.GONE else View.VISIBLE
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
        onBackPressed()
    }

    private fun updateViewsWithValidation() {
        if(isValidated) {
            tvStatus.text = "VALIDATED"
            btnValidate.text = "Invalidate"
            btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_clear_white,0)
            tvStatus.setTextColor(resources.getColor(R.color.yesGreen))
            tvValidatedAt.text = DATE_FORMAT_WITH_HOUR.format(Date())
            tvValidatedBy.text = userName
        }
        else {
            tvStatus.text = "NOT VALIDATED"
            btnValidate.text = "Validate"
            btnValidate.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_check_white,0)
            tvStatus.setTextColor(resources.getColor(R.color.darkerGrey))
            tvValidatedAt.text = "-"
            tvValidatedBy.text = "-"
        }
    }

    private fun updateTicketInfo(ticket : Ticket) {
        findViewById<ProgressBar>(R.id.lsSoldTo).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsSoldBy).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsValidatedAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsValidatedBy).visibility = View.INVISIBLE

        tvSoldTo.text = if (ticket.soldTo.isEmpty()) "-" else ticket.soldTo
        tvSoldAt.text = if (ticket.soldAt != null) DATE_FORMAT_WITH_HOUR.format(ticket.soldAt) else "-"
        if(ticket.soldAt != null) {
            if(ticket.soldBy != null) {
                tvSoldBy.text = ticket.soldBy.name
            }
            else {
                tvSoldBy.text = "?"
            }
        }
        else {
            tvSoldBy.text = "-"
        }

        if(ticket.validatedAt != null) {
            tvValidatedAt.text = DATE_FORMAT_WITH_HOUR.format(ticket.validatedAt)
            isValidated = true
        }
        else {
            tvValidatedAt.text = "-"
            isValidated = false
        }
        updateViewsWithValidation()
        if(ticket.validatedAt != null) {
            if(ticket.validatedBy != null) {
                tvValidatedBy.text = ticket.validatedBy.name
            }
            else {
                tvValidatedAt.text = "?"
            }
        }
        else {
            tvValidatedBy.text = "-"
        }
    }

    private fun onErrorResponse(title : String, responseCode : Int) {
        when(responseCode) {
            -1 -> {
                val dialogConnectionError = DialogInfo.newInstance("Connection error", "There was an error connecting to the server!", DialogType.ERROR)
                dialogConnectionError.show(supportFragmentManager, "DIALOG_ERROR")
            }
            401 -> {
                val dialogAuthError = DialogInfo.newInstance("Session expired", "You need to provide your authentication once again!", DialogType.AUTH_ERROR)
                dialogAuthError.isCancelable = false
                dialogAuthError.show(supportFragmentManager, "DIALOG_ERROR")
            }
            403 -> {
                val dialogError = DialogInfo.newInstance(title, "You are not allowed to see ticket information!", DialogType.ERROR)
                dialogError.show(supportFragmentManager, "DIALOG_ERROR")
            }
            404 -> {
                val dialogNoTicket = DialogInfo.newInstance(title, "The ticket you are trying to access no longer exists!", DialogType.ERROR)
                dialogNoTicket.dialogExitListener = this@ActivityTicketDetails
                dialogNoTicket.show(supportFragmentManager, "DIALOG_ERROR")
            }
        }
    }
}
