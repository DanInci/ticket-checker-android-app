package ticket.checker.admin.users

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
import ticket.checker.ActivityAdmin
import ticket.checker.R
import ticket.checker.AppTicketChecker
import ticket.checker.beans.User
import ticket.checker.listeners.DialogExitListener
import ticket.checker.dialogs.DialogInfo
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util.DATE_FORMAT_WITH_HOUR
import ticket.checker.extras.Util.POSITION
import ticket.checker.extras.Util.ROLE_ADMIN
import ticket.checker.extras.Util.USER_ID
import ticket.checker.extras.Util.USER_NAME
import ticket.checker.extras.Util.USER_ROLE
import ticket.checker.extras.Util.treatBasicError
import ticket.checker.services.ServiceManager

class ActivityUserDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {

    private var userId: Long = -1
    private var userName: String? = null
    private var userRole: String? = null
    private var userPosition: Int = -1
    private var isAdmin: Boolean = false

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvRole: TextView by lazy {
        findViewById<TextView>(R.id.role)
    }
    private val tvCreatedAt: TextView by lazy {
        findViewById<TextView>(R.id.createdAt)
    }
    private val tvTicketsCreated: TextView by lazy {
        findViewById<TextView>(R.id.ticketsCreated)
    }
    private val tvTicketsValidated: TextView by lazy {
        findViewById<TextView>(R.id.ticketsValidated)
    }
    private val btnRemove: Button by lazy {
        findViewById<Button>(R.id.btnRemove)
    }
    private val btnBack: ImageView by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val loadingSpinner: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }

    private val userCallback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if (response.isSuccessful) {
                when(method) {
                    "GET" -> {
                        updateUserInfo(response.body() as User)
                    }
                    "DELETE" -> {
                        loadingSpinner.visibility = View.GONE
                        val dialogDeleteSuccessful = DialogInfo.newInstance("Deletion successful", "User '$userName' was successfully deleted", DialogType.SUCCESS)
                        dialogDeleteSuccessful.dialogExitListener = this@ActivityUserDetails
                        dialogDeleteSuccessful.show(supportFragmentManager, "DIALOG_DELETE_SUCCESSFUL")
                    }
                }
            }
            else {
                switchToLoadingView(false)
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<T>, t: Throwable?) {
            switchToLoadingView(false)
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getLongExtra(USER_ID, -1)
        userName = intent.getStringExtra(USER_NAME)
        userRole = intent.getStringExtra(USER_ROLE)
        userPosition = intent.getIntExtra(POSITION, -1)
        setContentView(R.layout.activity_user_details)
        (findViewById<TextView>(R.id.toolbarTitle) as TextView).text = userName

        isAdmin = userRole == ROLE_ADMIN
        if (isAdmin) {
            tvRole.text = "ADMIN"
            tvRole.setTextColor(resources.getColor(R.color.yesGreen))
        } else {
            tvRole.text = "USER"
            tvRole.setTextColor(resources.getColor(R.color.darkerGrey))
        }

        setSupportActionBar(toolbar)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getUserService().getUsersById(userId)
        call.enqueue(userCallback as Callback<User>)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnRemove -> {
                val dialog: DialogInfo?
                when {
                    userId == AppTicketChecker.userId -> {
                        dialog = DialogInfo.newInstance("Delete failed", "You can not delete yourself", DialogType.ERROR)
                        dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                    }
                    isAdmin -> {
                        dialog = DialogInfo.newInstance("Delete failed", "You are not allowed to delete another admin", DialogType.ERROR)
                        dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                    }
                    else -> {
                        dialog = DialogInfo.newInstance("Confirm deletion", "Are you sure you want to delete user $userName ?", DialogType.YES_NO)
                        dialog.dialogResponseListener = this
                        dialog.show(supportFragmentManager, "DIALOG_CONFIRM_DELETE")
                    }
                }
            }
        }
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            switchToLoadingView(true)
            val call = ServiceManager.getUserService().deleteUserById(userId)
            call.enqueue(userCallback as Callback<Void>)
        }
    }

    override fun onItemRemoved() {
        val data = Intent()
        data.putExtra(POSITION, userPosition)
        setResult(ActivityAdmin.ITEM_REMOVED, data)
        onBackPressed()
    }

    private fun switchToLoadingView(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRemove.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateUserInfo(user: User) {
        findViewById<ProgressBar>(R.id.lsCreatedAt).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsTicketsCreated).visibility = View.INVISIBLE
        findViewById<ProgressBar>(R.id.lsTicketsValidated).visibility = View.INVISIBLE

        tvCreatedAt.text = DATE_FORMAT_WITH_HOUR.format(user.createdDate)
        tvTicketsCreated.text = "${user.soldTicketsNo}"
        tvTicketsValidated.text = "${user.validatedTicketsNo}"
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = treatBasicError(call, response, supportFragmentManager)
        if (!wasHandled) {
            if (response?.code() == 400) {
                val dialogNoTicket = DialogInfo.newInstance("User not found", "The user you are trying to access no longer exists", DialogType.ERROR)
                dialogNoTicket.dialogExitListener = this@ActivityUserDetails
                dialogNoTicket.show(supportFragmentManager, "DIALOG_ERROR")
            }
        }
    }
}
