package ticket.checker

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.LoginData
import ticket.checker.beans.LoginResponse
import ticket.checker.dialogs.DialogInfo
import ticket.checker.extras.DialogType
import ticket.checker.extras.Util
import ticket.checker.extras.safeLet
import ticket.checker.services.ServiceManager


class ActivityLogin : AppCompatActivity(), View.OnClickListener {

    private var savedEmail : String = ""
    private var savedPassword : String = ""

    private val etEmail by lazy { findViewById<EditText>(R.id.etEmail)}
    private val etPassword by lazy { findViewById<EditText>(R.id.etPassword)}
    private val btnLogin by lazy { findViewById<Button>(R.id.btnLogin)}
    private val btnToRegister by lazy { findViewById<Button>(R.id.btnToRegister)}
    private val autoLoginCheckBox by lazy { findViewById<CheckBox>(R.id.autoLoginCheckBox)}

    private val loggingInDialog : DialogInfo by lazy {
        DialogInfo.newInstance("Logging in","Retrieving user info...", DialogType.LOADING)
    }

    private val loginCallback : Callback<LoginResponse> = object : Callback<LoginResponse> {
        override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
            loggingInDialog.dismiss()
            btnLogin.isClickable = true
            btnToRegister.isClickable = true
            if(response.isSuccessful) {
                if(autoLoginCheckBox.isChecked) {
                    AppTicketChecker.saveSession(savedEmail, savedPassword)
                    savedEmail = ""
                    savedPassword = ""
                }
                loggedIn(response.body() as LoginResponse)
            }
            else {
                ServiceManager.invalidateSession()
                when(response.code()) {
                    401,403 -> {
                        val loginFailedDialog = DialogInfo.newInstance("Login failed","The email or password you entered was incorrect!", DialogType.ERROR)
                        loginFailedDialog.show(supportFragmentManager,"DIALOG_LOGIN_FAILED")
                    }
                    else -> {
                        Util.treatBasicError(call, response, supportFragmentManager)
                    }
                }
            }
        }

        override fun onFailure(call: Call<LoginResponse>, t: Throwable?) {
            loggingInDialog.dismiss()
            btnLogin.isClickable = true
            btnToRegister.isClickable = true
            Util.treatBasicError(call, null, supportFragmentManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loadLogoImage()
        safeLet(AppTicketChecker.savedSessionEmail, AppTicketChecker.savedSessionPassword) { e, p ->
            this.etEmail.setText(e)
            this.etPassword.setText(p)
            this.autoLoginCheckBox.isChecked = true
            loggingInDialog.show(supportFragmentManager, "DIALOG_LOGGING_IN")
            doLogin(etEmail.text.toString(), etPassword.text.toString())
            btnLogin.isClickable = false
            btnToRegister.isClickable = false
        }
    }

    override fun onStart() {
        super.onStart()
        initialize()
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnLogin -> {
                if(validate(etEmail, etPassword)) {
                    loggingInDialog.show(supportFragmentManager, "DIALOG_LOGGING_IN")
                    doLogin(etEmail.text.toString(), etPassword.text.toString())
                    btnLogin.isClickable = false
                    btnToRegister.isClickable = false
                }
            }
            R.id.btnToRegister -> {
                toRegisterActivity()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when(requestCode) {
            REGISTER_ACTIVITY -> {
                if(resultCode == Activity.RESULT_OK && data != null) {
                    savedEmail = data.getStringExtra("result")!!
                }
            }
        }

    }

    private fun validate(vararg ets : EditText) : Boolean {
        var isValid = true
        for(et in ets) {
            if(et.text.isEmpty()) {
                isValid = false
                et.error = "This field can not be empty!"
            }
            else {
                when(et.id) {
                    R.id.etEmail -> {
                        if(!Util.isEmailValid(et.text.toString())) {
                            isValid = false
                            et.error = "Email format is not correct"
                        }
                    }
                }
            }
        }
        return isValid
    }

    private fun loadLogoImage() {
        val imgView = findViewById<ImageView>(R.id.imgLogo)
        val baseUrl = ServiceManager.API_BASE_URL
        Glide.with(applicationContext)
                .asBitmap()
                .load("${baseUrl}images/logo.png")
                .centerCrop()
                .into(imgView)
    }

    private fun initialize() {
        etEmail.setText(savedEmail)
        etEmail.error = null
        etEmail.requestFocus()
        etPassword.setText("")
        etPassword.error = null
        btnLogin.setOnClickListener(this)
        btnToRegister.setOnClickListener(this)
        autoLoginCheckBox.isChecked = false
    }

    private fun doLogin(email: String, password: String) {
        if(autoLoginCheckBox.isChecked) {
            savedEmail = email
            savedPassword = password
        }

        val call : Call<LoginResponse> = ServiceManager.getAuthService().login(LoginData(email, password))
        call.enqueue(loginCallback)
    }

    private fun loggedIn(response : LoginResponse) {
        AppTicketChecker.loggedInUser = response.profile
        ServiceManager.createSession(response.token)
        toOrganizationsActivity()
    }

    private fun toOrganizationsActivity() {
        val intent = Intent(this, ActivityOrganizations::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    private fun toRegisterActivity() {
        val intent = Intent(this, ActivityRegister::class.java)
        startActivityForResult(intent, REGISTER_ACTIVITY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(EMAIL_TEXT, etEmail.text.toString())
        outState.putString(PASS_TEXT, etPassword.text.toString())
        outState.putBoolean(AUTO_LOGIN, autoLoginCheckBox.isChecked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastEtUser = savedInstanceState.getString(EMAIL_TEXT)
        val lastEtPass = savedInstanceState.getString(PASS_TEXT)
        val isChecked = savedInstanceState.getBoolean(AUTO_LOGIN, false)
        etEmail.setText(lastEtUser)
        etPassword.setText(lastEtPass)
        autoLoginCheckBox.isChecked = isChecked
    }

    companion object {
        private const val REGISTER_ACTIVITY = 1
        private const val EMAIL_TEXT = "emailEtText"
        private const val PASS_TEXT = "passEtText"
        private const val AUTO_LOGIN = "autoLogin"
    }
}
