package ticket.checker

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.UserProfile
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util
import androidx.appcompat.widget.Toolbar
import ticket.checker.beans.UserDefinition
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager
import java.util.*

class ActivityProfile : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {

    private lateinit var userId: UUID

    private var itemWasRemoved = false

    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvEmail by lazy {
        findViewById<TextView>(R.id.tvEmail)
    }
    private val tvName by lazy {
        findViewById<TextView>(R.id.tvName)
    }
    private val etName by lazy {
        findViewById<EditText>(R.id.etName)
    }
    private val nameTvContainer by lazy {
        findViewById<LinearLayout>(R.id.nameTvContainer)
    }
    private val nameEtContainer by lazy {
        findViewById<LinearLayout>(R.id.nameEtContainer)
    }
    private val tvCreatedAt by lazy {
        findViewById<TextView>(R.id.tvCreatedAt)
    }
    private val btnConfirmName by lazy {
        findViewById<ImageButton>(R.id.btnConfirmName)
    }
    private val btnEditName by lazy {
        findViewById<ImageButton>(R.id.btnEditName)
    }
    private val btnDelete by lazy {
        findViewById<Button>(R.id.btnDelete)
    }
    private val btnBack by lazy {
        findViewById<ImageButton>(R.id.btnBack)
    }
    private val loadingSpinner by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }

    private val callback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if (response.isSuccessful) {
                when(method) {
                    "GET", "PUT" -> {
                        updateProfileInfo(response.body() as UserProfile)
                        btnDelete.visibility = View.VISIBLE
                    }
                    "DELETE" -> {
                        loadingSpinner.visibility = View.GONE
                        val dialogDeleteSuccessful = DialogInfo.newInstance("Deletion successful", "Your account has been deleted!", DialogType.SUCCESS)
                        dialogDeleteSuccessful.dialogExitListener = this@ActivityProfile
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
        setContentView(R.layout.activity_profile)

        this.userId = intent.getSerializableExtra(USER_ID) as UUID

        setSupportActionBar(toolbar)
        btnConfirmName.setOnClickListener(this)
        btnEditName.setOnClickListener(this)
        btnDelete.setOnClickListener(this)
        btnBack.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        reloadProfileInfo()
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnConfirmName -> {
                val etText = etName.text.toString()
                if(etText != "") {
                    val definition = UserDefinition(etText)
                    val call = ServiceManager.getUserService().updateUserById(userId, definition)
                    call.enqueue(callback as Callback<UserProfile>)
                }
                switchToEditName(false)
            }
            R.id.btnEditName -> {
                switchToEditName(true)
            }
            R.id.btnDelete -> {
                val dialog = DialogInfo.newInstance("Confirm deletion", "Are you sure you want to delete your account?", DialogType.YES_NO)
                dialog.dialogResponseListener = this
                dialog.show(supportFragmentManager, "DIALOG_CONFIRM_DELETE_USER")
            }
            R.id.btnBack -> {
                onBackPressed()
            }
        }
    }

    override fun onBackPressed() {
        if (itemWasRemoved) {
            AppTicketChecker.clearSession()
            val intent = Intent(this, ActivityLogin::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
            finish()
        }
        else {
            finish()
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
        super.onBackPressed()
    }

    override fun onResponse(response: Boolean) {
        if (response) {
            switchToLoadingView(true)
            val call = ServiceManager.getUserService().deleteUserById(userId)
            call.enqueue(callback as Callback<Void>)
        }
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
        onBackPressed()
    }

    private fun reloadProfileInfo() {
        findViewById<Button>(R.id.btnDelete).visibility = View.GONE
        findViewById<ProgressBar>(R.id.loadingSpinner).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.lsEmail).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.lsName).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.lsCreatedAt).visibility = View.VISIBLE

        nameTvContainer.visibility = View.GONE
        nameEtContainer.visibility = View.GONE

        tvEmail.text = ""
        tvName.text = ""
        tvCreatedAt.text = ""

        val call = ServiceManager.getUserService().getUserById(userId)
        call.enqueue(callback as Callback<UserProfile>)
    }

    private fun switchToEditName(isInEditMode: Boolean) {
        nameTvContainer.visibility = if (isInEditMode) View.GONE else View.VISIBLE
        nameEtContainer.visibility = if (isInEditMode) View.VISIBLE else View.GONE

        if(isInEditMode) {
            etName.setText(tvName.text)
        } else {
            tvName.setText(etName.text.toString())
        }
    }

    private fun switchToLoadingView(isLoading: Boolean) {
        loadingSpinner.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnDelete.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun updateProfileInfo(user: UserProfile) {
        findViewById<Button>(R.id.btnDelete).visibility = View.VISIBLE
        findViewById<ProgressBar>(R.id.loadingSpinner).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsEmail).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsName).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsCreatedAt).visibility = View.GONE

        nameTvContainer.visibility = View.VISIBLE
        nameEtContainer.visibility = View.GONE

        tvEmail.text = user.email
        tvName.text = user.name
        tvCreatedAt.text = Util.DATE_TIME_FORMAT.format(user.createdAt)

        AppTicketChecker.loggedInUser = user
    }

    private fun <T> onErrorResponse(call: Call<T>, response: Response<T>?) {
        val wasHandled = Util.treatBasicError(call, response, supportFragmentManager)
        if (!wasHandled) {
            when(response?.code()) {
                400 -> {
                    val dialogNoOrganization = DialogInfo.newInstance("User not found", "The user you are trying to access no longer exists", DialogType.ERROR)
                    dialogNoOrganization.dialogExitListener = this@ActivityProfile
                    dialogNoOrganization.show(supportFragmentManager, "DIALOG_ERROR")
                }
                409 -> {
                    val dialogNoOrganization = DialogInfo.newInstance("Cannot delete account", "You are the owner of some organizations. Please delete those organizations first", DialogType.ERROR)
                    dialogNoOrganization.dialogExitListener = this@ActivityProfile
                    dialogNoOrganization.show(supportFragmentManager, "DIALOG_ERROR")
                }
                else -> {
                    val dialogNoOrganization = DialogInfo.newInstance("Unknown error", "An unknown error has happened", DialogType.ERROR)
                    dialogNoOrganization.dialogExitListener = this@ActivityProfile
                    dialogNoOrganization.show(supportFragmentManager, "DIALOG_ERROR")
                }
            }
        }
    }

    companion object {
        const val USER_ID = "userId"
    }

}
