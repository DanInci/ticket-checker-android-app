package ticket.checker

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.view.View
import android.widget.*
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.AppTicketChecker.Companion.isLoggedIn
import ticket.checker.AppTicketChecker.Companion.loggedInUserCreatedDate
import ticket.checker.AppTicketChecker.Companion.loggedInUserId
import ticket.checker.AppTicketChecker.Companion.loggedInUserName
import ticket.checker.AppTicketChecker.Companion.loggedInUserSoldTicketsNo
import ticket.checker.AppTicketChecker.Companion.loggedInUserType
import ticket.checker.AppTicketChecker.Companion.loggedInUserValidatedTicketsNo
import ticket.checker.AppTicketChecker.Companion.pretendedUserType
import ticket.checker.beans.User
import ticket.checker.dialogs.DialogConnectionConfig
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util
import ticket.checker.services.ServiceManager

class ActivityLogin : AppCompatActivity(), View.OnClickListener {
    private var savedUser : String = ""
    private var savedPass : String = ""

    private val etUsername : EditText by lazy { findViewById<EditText>(R.id.etUsername)}
    private val etPassword : EditText by lazy { findViewById<EditText>(R.id.etPassword)}
    private val btnLogin : Button by lazy { findViewById<Button>(R.id.btnLogin)}
    private val btnSettings : ImageButton by lazy { findViewById<ImageButton>(R.id.btnSettings)}
    private val autoLoginCheckBox : CheckBox by lazy { findViewById<CheckBox>(R.id.autoLoginCheckBox)}

    private val loggingInDialog : DialogInfo by lazy {
        DialogInfo.newInstance("Logging in","Retrieving user info...",DialogType.LOADING)
    }

    private val loginCallback : Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            loggingInDialog.dismiss()
            btnLogin.isClickable = true
            if(response.isSuccessful) {
                if(autoLoginCheckBox.isChecked) {
                    val encryptedPass = Util.hashString("SHA-256",savedPass)
                    AppTicketChecker.saveSession(savedUser, encryptedPass)
                    savedUser = ""
                    savedPass = ""
                }
                login(response.body() as User)
            }
            else {
                ServiceManager.invalidateSession()
                when(response.code()) {
                    401,403 -> {
                        val loginFailedDialog = DialogInfo.newInstance("Login failed","The username or password you entered was incorrect!",DialogType.ERROR)
                        loginFailedDialog.show(supportFragmentManager,"DIALOG_LOGIN_FAILED")
                    }
                    else -> {
                        Util.treatBasicError(call, response, supportFragmentManager)
                    }
                }
            }
        }
        override fun onFailure(call: Call<User>, t: Throwable?) {
            loggingInDialog.dismiss()
            btnLogin.isClickable = true
            Util.treatBasicError(call, null, supportFragmentManager)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        if(AppTicketChecker.address != "") {
            if(AppTicketChecker.isLoggedIn) {
                toMenuActivity()
            }
        }
        loadBgnImage()
    }

    override fun onStart() {
        super.onStart()
        initialize()
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnSettings -> {
                val connectionConfigureDialog = DialogConnectionConfig.newInstance(AppTicketChecker.address,AppTicketChecker.port)
                connectionConfigureDialog.show(supportFragmentManager, "DIALOG_CONNECTION_CONFIGURE")
            }
            R.id.btnLogin -> {
                if(validate(etUsername,etPassword)) {
                    if(AppTicketChecker.address != "") {
                        loggingInDialog.show(supportFragmentManager, "DIALOG_LOGGING_IN")
                        doLogin(etUsername.text.toString(), etPassword.text.toString())
                        btnLogin.isClickable = false
                    }
                    else {
                        val connectionConfigureDialog = DialogConnectionConfig.newInstance(AppTicketChecker.address,AppTicketChecker.port)
                        connectionConfigureDialog.show(supportFragmentManager, "DIALOG_CONNECTION_CONFIGURE")
                    }
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
        }
        return isValid
    }

    private fun loadBgnImage() {
        val bgnImg = R.drawable.bg_gradient
        val imgView = findViewById<ImageView>(R.id.imgBgn)
        Glide.with(this)
                .load(bgnImg)
                .apply(RequestOptions().centerCrop())
                .into(imgView)
    }

    private fun initialize() {
        etUsername.setText("")
        etUsername.error = null
        etUsername.requestFocus()
        etPassword.setText("")
        etPassword.error = null
        btnLogin.setOnClickListener(this)
        btnSettings.setOnClickListener(this)
        autoLoginCheckBox.isChecked = false
    }

    private fun doLogin(username: String, password: String) {
        ServiceManager.createSession(username, password, true)
        if(autoLoginCheckBox.isChecked) {
            savedUser = username
            savedPass = password
        }
        val call : Call<User> = ServiceManager.getUserService().getUser()
        call.enqueue(loginCallback)
    }

    private fun login(user : User) {
        loggedInUserId = user.userId
        loggedInUserName = user.name
        loggedInUserType = user.userType
        loggedInUserCreatedDate = user.createdAt
        loggedInUserSoldTicketsNo = user.soldTicketsNo
        loggedInUserValidatedTicketsNo = user.validatedTicketsNo
        pretendedUserType = user.userType
        isLoggedIn = true
        toMenuActivity()
    }

    private fun toMenuActivity() {
        val intent = Intent(this, ActivityMenu::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(USER_TEXT,etUsername.text.toString())
        outState.putString(PASS_TEXT,etPassword.text.toString())
        outState.putBoolean(AUTO_LOGIN, autoLoginCheckBox.isChecked)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastEtUser = savedInstanceState.getString(USER_TEXT)
        val lastEtPass = savedInstanceState.getString(PASS_TEXT)
        val isChecked = savedInstanceState.getBoolean(AUTO_LOGIN, false)
        etUsername.setText(lastEtUser)
        etPassword.setText(lastEtPass)
        autoLoginCheckBox.isChecked = isChecked
    }

    companion object {
        private const val USER_TEXT = "userEtText"
        private const val PASS_TEXT = "passEtText"
        private const val AUTO_LOGIN = "autoLogin"
    }
}
