package ticket.checker.admin.users

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
import ticket.checker.R
import ticket.checker.admin.listeners.EditListener
import ticket.checker.beans.Ticket
import ticket.checker.beans.User
import ticket.checker.extras.BirthDateFormatException
import ticket.checker.extras.BirthDateIncorrectException
import ticket.checker.extras.UserType
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager
import java.util.*

class DialogEditUser : DialogFragment(), View.OnClickListener {
    var editListener: EditListener<User>? = null

    private var userId : Long? = 0
    private var name : String? = null

    private var btnClose: ImageButton? = null
    private var tvTitle: TextView? = null
    private var etName: EditText? = null
    private var spinnerRole: Spinner? = null
    private var bottomContainer : LinearLayout? = null
    private var editButton: Button? = null
    private var loadingSpinner: ProgressBar? = null
    private var tvResult: TextView? = null

    private val getCallback : Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            if (response.isSuccessful) {
                loadingSpinner?.visibility = View.GONE
                editButton?.visibility = View.VISIBLE
                updateWithUserInfo(response.body() as User)
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<User>, t: Throwable) {
            loadingSpinner?.visibility = View.GONE
            onErrorResponse(call, null)
        }
    }

    private val editCallback: Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            loadingSpinner?.visibility = View.GONE
            editButton?.visibility = View.VISIBLE
            if (response.isSuccessful) {
                editListener?.onEdit(response.body() as User)
                dismiss()
            } else {
                onErrorResponse(call, response)
            }
        }
        override fun onFailure(call: Call<User>, t: Throwable) {
            loadingSpinner?.visibility = View.GONE
            editButton?.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            userId = arguments?.getLong(USER_ID, 0)
            name = arguments?.getString(NAME) ?: "NONE"
        }
    }

    override fun onStart() {
        super.onStart()
        val getCall = ServiceManager.getUserService().getUsersById(userId as Long)
        getCall.enqueue(getCallback)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_edit_user, container, false)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        tvTitle = view.findViewById(R.id.dialogTitle)
        tvTitle?.text = "Edit '$name'"
        etName = view.findViewById(R.id.etName)
        spinnerRole = view.findViewById(R.id.spinnerRole)
        bottomContainer = view.findViewById(R.id.bottomContainer)
        editButton = view.findViewById(R.id.btnEdit)
        editButton?.setOnClickListener(this)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvResult = view.findViewById(R.id.tvResult)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnEdit -> {
                if (validate()) {
                    editUser(userId as Long, etName?.text.toString(), spinnerRole?.selectedItem as UserType)
                }
            }
        }
    }

    private fun updateWithUserInfo(user : User) {
        etName?.isEnabled = true
        etName?.setText(user.name)
        etName?.error = null
        etName?.post( { etName?.setSelection(user.name.length) })
        spinnerRole?.isClickable = true
        spinnerRole?.isFocusable = true
        spinnerRole?.adapter = ArrayAdapter<UserType>(context, R.layout.spinner_item, UserType.values())
        spinnerRole?.setSelection(user.userType.ordinal)
    }

    private fun validate() : Boolean {
        tvResult?.visibility = View.INVISIBLE
        val name = etName?.text.toString()
        if(name.isEmpty()) {
            etName?.error = "This field is required"
            return false
        }
        return true
    }

    private fun editUser(userId : Long, name: String, userType : UserType) {
        tvResult?.visibility = View.INVISIBLE
        editButton?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE

        val user = User(userId, "blabla", "blabla", name, UserType.fromUserTypeToRole(userType), null,0,0)
        val call = ServiceManager.getUserService().editUser(userId, user)
        call.enqueue(editCallback)
    }

    private fun onErrorResponse(call: Call<User>, response: Response<User>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager)
        if (!wasHandled) {
            if (response?.code() == 404) {
                bottomContainer?.visibility = View.GONE
                tvResult?.visibility = View.VISIBLE
                tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.noRed))
                tvResult?.text = "The user was not found!"
            }
        }
    }

    companion object {
        private const val NAME = "name"
        private const val USER_ID = "userId"

        fun newInstance(userId : Long, name: String): DialogEditUser {
            val fragment = DialogEditUser()
            val args = Bundle()
            args.putLong(USER_ID, userId)
            args.putString(NAME, name)
            fragment.arguments = args
            return fragment
        }
    }

}