package ticket.checker.admin.users

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
import ticket.checker.beans.User
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util.ROLE_ADMIN
import ticket.checker.extras.Util.ROLE_USER
import ticket.checker.extras.Util.hashString
import ticket.checker.services.ServiceManager

class DialogAddUser : DialogFragment(), View.OnClickListener {
    var actionListener: ActionListener<User>? = null
    private val roles = hashMapOf("ADMIN" to ROLE_ADMIN, "USER" to ROLE_USER)

    private var btnClose : ImageButton? = null
    private var tvTitle : TextView? = null
    private var etUsername : EditText? = null
    private var etPassword : EditText? = null
    private var etPasswordRepeat : EditText? = null
    private var etName : EditText? = null
    private var spinnerRole : Spinner? = null
    private var submitButton : Button? = null
    private var loadingSpinner : ProgressBar? = null
    private var tvResult : TextView? = null

    private val submitCallback = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            loadingSpinner?.visibility = View.GONE
            submitButton?.visibility = View.VISIBLE
            if(response.isSuccessful) {
                actionListener?.onAdd(response.body() as User)
                tvResult?.visibility = View.VISIBLE
                tvResult?.setTextColor(context.resources.getColor(R.color.yesGreen))
                tvResult?.text = "You have added the user successfully"
                resetFields()
            }
            else {
                when(response.code()) {
                    400 -> {
                        etUsername?.error = "This username already exists!"
                    }
                    401 -> {
                        dismiss()
                        val authDialog = DialogInfo.newInstance("Session expired","You need to provide your authentication once again!", DialogType.AUTH_ERROR)
                        authDialog.isCancelable = false
                        authDialog.show(fragmentManager,"DIALOG_AUTH_ERROR")
                    }
                    403 -> {
                        val permissionDialog = DialogInfo.newInstance("Add failed","You don't have permissions to add users!", DialogType.ERROR)
                        permissionDialog.show(fragmentManager,"DIALOG_FAIL")
                    }
                    else -> {
                        val unknownError = DialogInfo.newInstance("Add failed", "There was an unexpected error!", DialogType.ERROR)
                        unknownError.show(fragmentManager, "DIALOG_FAIL")
                    }
                }
            }
        }

        override fun onFailure(call: Call<User>?, t: Throwable?) {
            loadingSpinner?.visibility = View.GONE
            submitButton?.visibility = View.VISIBLE
            val dialogConnection = DialogInfo.newInstance("Connection error","There was an error connecting to the server", DialogType.ERROR)
            dialogConnection.showHeader(false)
            dialogConnection.show(fragmentManager,"DIALOG_FAIL")
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater!!.inflate(R.layout.dialog_add_user, container, false)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        tvTitle = view.findViewById(R.id.tvTitle)
        etUsername = view.findViewById(R.id.etUsername)
        etPassword = view.findViewById(R.id.etPassword)
        etPasswordRepeat = view.findViewById(R.id.etPasswordRepeat)
        etName = view.findViewById(R.id.etName)
        spinnerRole = view.findViewById(R.id.spinnerRole)
        submitButton = view.findViewById(R.id.btnSubmit)
        submitButton?.setOnClickListener(this)
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        tvResult = view.findViewById(R.id.tvResult)
        setupLoadingSpinner()
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        dialog?.setCanceledOnTouchOutside(false)
        return view
    }

    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnSubmit -> {
                if(validate()) {
                    submitUser(etUsername?.text.toString(), etPassword?.text.toString(), etName?.text.toString(), roles.getOrElse(spinnerRole?.selectedItem as String, { ROLE_USER }))
                }
            }
        }
    }

    private fun setupLoadingSpinner() {
        val adapter = ArrayAdapter<String>(context, R.layout.spinner_item, roles.keys.toTypedArray())
        spinnerRole?.adapter = adapter
    }

    private fun resetFields() {
        etUsername?.setText("")
        etUsername?.error = null
        etUsername?.requestFocus()
        etPassword?.setText("")
        etPassword?.error = null
        etPasswordRepeat?.setText("")
        etPasswordRepeat?.error = null
        etName?.setText("")
        etName?.error = null
        spinnerRole?.setSelection(0)
    }

    private fun validate() : Boolean {
        val username = etUsername?.text.toString()
        val password = etPassword?.text.toString()
        val repeatPass = etPasswordRepeat?.text.toString()
        val name = etName?.text.toString()

        var isValid = true

        if(username.isEmpty()) {
            etUsername?.error = "This field is required"
            isValid = false
        }
        if(password.isEmpty()) {
            etPassword?.error = "This field is required"
            isValid = false
        }
        if(repeatPass.isEmpty()) {
            etPasswordRepeat?.error = "This field is required"
            isValid = false
        }
        else {
            if(!password.isEmpty() && password != repeatPass) {
                etPasswordRepeat?.error = "Passwords don't match"
                isValid = false
            }
        }
        if(name.isEmpty()) {
            etName?.error = "This field is required"
            isValid = false
        }
        return isValid
    }

    private fun submitUser(username : String, password : String, name : String, role : String) {
        tvResult?.visibility = View.INVISIBLE
        submitButton?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE

        val encryptedPassword = hashString("SHA-256", password)
        val user = User(username, encryptedPassword, name , role)
        val call = ServiceManager.getUserService().createUser(user)
        call.enqueue(submitCallback)
    }
}
