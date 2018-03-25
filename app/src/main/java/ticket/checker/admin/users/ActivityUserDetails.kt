package ticket.checker.admin.users

import android.content.Intent
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.ActivityControlPanel
import ticket.checker.ActivityControlPanel.Companion.EDITED_OBJECT
import ticket.checker.ActivityControlPanel.Companion.ITEM_EDITED
import ticket.checker.ActivityControlPanel.Companion.ITEM_REMOVED
import ticket.checker.AppTicketChecker
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.User
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.UserType
import ticket.checker.extras.Util.CURRENT_USER
import ticket.checker.extras.Util.DATE_FORMAT_WITH_HOUR
import ticket.checker.extras.Util.POSITION
import ticket.checker.extras.Util.treatBasicError
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager

class ActivityUserDetails : AppCompatActivity(), View.OnClickListener, DialogExitListener, DialogResponseListener {
    private var isFirstLoad = true
    private var itemsWasEdited = false
    private var itemWasRemoved = false

    private var currentUser : User? = null
    private val userPosition : Int by lazy {
        intent.getIntExtra(POSITION, -1)
    }

    private val toolbar: Toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val tvTitle : TextView by lazy {
        findViewById<TextView>(R.id.toolbarTitle)
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
    private val btnEdit : ImageButton by lazy {
        findViewById<ImageButton>(R.id.btnEdit)
    }
    private val btnRemove: Button by lazy {
        findViewById<Button>(R.id.btnRemove)
    }
    private val btnBack: ImageButton by lazy {
        findViewById<ImageButton>(R.id.btnBack)
    }
    private val loadingSpinner: ProgressBar by lazy {
        findViewById<ProgressBar>(R.id.loadingSpinner)
    }

    private val editListener : EditListener<User> = object : EditListener<User> {
        override fun onEdit(editedObject: User) {
            itemsWasEdited = true
            updateUserInfo(editedObject)
        }
    }

    private val userCallback = object <T> : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            val method = call.request().method()
            if (response.isSuccessful) {
                when(method) {
                    "GET" -> {
                        updateUserInfo(response.body() as User)
                        if(isFirstLoad) {
                            isFirstLoad = false
                            btnRemove.visibility = View.VISIBLE
                        }
                    }
                    "DELETE" -> {
                        loadingSpinner.visibility = View.GONE
                        val dialogDeleteSuccessful = DialogInfo.newInstance("Deletion successful", "User '${currentUser?.name}' was successfully deleted", DialogType.SUCCESS)
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
        setContentView(R.layout.activity_user_details)

        val user  = savedInstanceState?.getString(CURRENT_USER) as User? ?: intent.getSerializableExtra(CURRENT_USER) as User
        updateUserInfo(user)

        setSupportActionBar(toolbar)
        btnRemove.setOnClickListener(this)
        btnBack.setOnClickListener(this)
        btnEdit.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        val call = ServiceManager.getUserService().getUsersById(currentUser?.id as Long)
        call.enqueue(userCallback as Callback<User>)
    }

    override fun onBackPressed() {
        if (itemWasRemoved) {
            val data = Intent()
            data.putExtra(POSITION, userPosition)
            setResult(ITEM_REMOVED, data)
        }
        else {
            if(itemsWasEdited) {
                val data = Intent()
                data.putExtra(POSITION, userPosition)
                data.putExtra(EDITED_OBJECT, currentUser)
                setResult(ITEM_EDITED, data)
            }
        }
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putSerializable(CURRENT_USER, currentUser)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> {
                onBackPressed()
            }
            R.id.btnEdit -> {
                val editDialog = DialogEditUser.newInstance(currentUser?.id!!, currentUser?.name!!)
                editDialog.editListener = editListener
                editDialog.show(supportFragmentManager, "DIALOG_EDIT_USER")
            }
            R.id.btnRemove -> {
                val dialog: DialogInfo?
                when {
                    currentUser?.id!! == AppTicketChecker.loggedInUserId -> {
                        dialog = DialogInfo.newInstance("Delete failed", "You can not delete yourself", DialogType.ERROR)
                        dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                    }
                    currentUser?.userType == UserType.ADMIN -> {
                        dialog = DialogInfo.newInstance("Delete failed", "You are not allowed to delete another admin", DialogType.ERROR)
                        dialog.show(supportFragmentManager, "DIALOG_NOT_ALLOWED")
                    }
                    else -> {
                        dialog = DialogInfo.newInstance("Confirm deletion", "Are you sure you want to delete user ${currentUser?.name} ?", DialogType.YES_NO)
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
            val call = ServiceManager.getUserService().deleteUserById(currentUser?.id!!)
            call.enqueue(userCallback as Callback<Void>)
        }
    }

    override fun onItemRemoved() {
        itemWasRemoved = true
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

        currentUser = user

        tvTitle.text = user.name
        tvRole.text = user.userType.role
        tvRole.setTextColor(ContextCompat.getColor(applicationContext, user.userType.colorResource))

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
