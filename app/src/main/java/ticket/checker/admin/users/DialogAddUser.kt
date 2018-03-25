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
import ticket.checker.admin.listeners.ListChangeListener
import ticket.checker.beans.User
import ticket.checker.extras.UserType
import ticket.checker.extras.Util
import ticket.checker.extras.Util.hashString
import ticket.checker.services.ServiceManager

class DialogAddUser : DialogFragment(), View.OnClickListener {
    var listChangeListener: ListChangeListener<User>? = null

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
                listChangeListener?.onAdd(response.body() as User)
                tvResult?.visibility = View.VISIBLE
                tvResult?.setTextColor(ContextCompat.getColor(context!!, R.color.yesGreen))
                tvResult?.text = "You have added the user successfully"
                resetFields()
            }
            else {
                onErrorResponse(call, response)
            }
        }

        override fun onFailure(call: Call<User>, t: Throwable?) {
            loadingSpinner?.visibility = View.GONE
            submitButton?.visibility = View.VISIBLE
            onErrorResponse(call, null)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view =  inflater.inflate(R.layout.dialog_add_user, container, false)
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
                    submitUser(etUsername?.text.toString(), etPassword?.text.toString(), etName?.text.toString(), spinnerRole?.selectedItem as UserType)
                }
            }
        }
    }

    private fun setupLoadingSpinner() {
        val adapter = ArrayAdapter<UserType>(context, R.layout.spinner_item, UserType.values())
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
        tvResult?.visibility = View.INVISIBLE

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

    private fun submitUser(username : String, password : String, name : String, userType : UserType) {
        tvResult?.visibility = View.INVISIBLE
        submitButton?.visibility = View.GONE
        loadingSpinner?.visibility = View.VISIBLE

        val encryptedPassword = hashString("SHA-256", password)
        val user = User(username, encryptedPassword, name , userType)
        val call = ServiceManager.getUserService().createUser(user)
        call.enqueue(submitCallback)
    }

    private fun onErrorResponse(call : Call<User>, response : Response<User>?) {
        val wasHandled = Util.treatBasicError(call, response, fragmentManager)
        if(!wasHandled){
            if(response?.code() == 400) {
                etUsername?.error = "This username already exists!"
            }
        }
    }
}
