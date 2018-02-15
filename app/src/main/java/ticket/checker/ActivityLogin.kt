package ticket.checker

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.User
import ticket.checker.dialogs.DialogInfo
import ticket.checker.dialogs.DialogType
import ticket.checker.extras.Util.userCreatedDate
import ticket.checker.extras.Util.userId
import ticket.checker.extras.Util.userName
import ticket.checker.extras.Util.userRole
import ticket.checker.extras.Util.userSoldTicketsNo
import ticket.checker.extras.Util.userValidatedTicketsNo
import ticket.checker.services.ServiceManager

class ActivityLogin : AppCompatActivity() {
    private val etUsername : EditText by lazy { findViewById<EditText>(R.id.etUsername)}
    private val etPassword : EditText by lazy { findViewById<EditText>(R.id.etPassword)}
    private val btnLogin : Button by lazy { findViewById<Button>(R.id.btnLogin)}

    private val loginHandler : View.OnClickListener = View.OnClickListener {
        if(validate(etUsername,etPassword)) {
            loggingInDialog.show(supportFragmentManager,"DIALOG_LOGGING_IN")
            doLogin(etUsername.text.toString(), etPassword.text.toString())
        }
    }

    private val loggingInDialog : DialogInfo by lazy {
        DialogInfo.newInstance("Logging in","Retrieving user info...",DialogType.LOADING)
    }

    private val loginCallback : Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>?, response: Response<User>) {
            loggingInDialog.dismiss()
            if(response.isSuccessful) {
                login(response.body() as User)
            }
            else {
                ServiceManager.invalidateSession()
                when(response.code()) {
                    401,403 -> {
                        val loginFailedDialog = DialogInfo.newInstance("Login failed","The username or password you entered was incorrect!",DialogType.ERROR)
                        loginFailedDialog.show(supportFragmentManager,"DIALOG_LOGIN_FAILED")
                    }
                    in 500..600 -> {
                        val loginFailedDialog = DialogInfo.newInstance("Login failed","There was a server error!",DialogType.ERROR)
                        loginFailedDialog.show(supportFragmentManager,"DIALOG_LOGIN_FAILED")
                    }
                }
            }
        }
        override fun onFailure(call: Call<User>?, t: Throwable?) {
            loggingInDialog.dismiss()
            val loginFailedDialog = DialogInfo.newInstance("Login failed","There was an error connecting to the server!", DialogType.ERROR)
            loginFailedDialog.show(supportFragmentManager,"DIALOG_LOGIN_FAILED")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        loadBgnImage()
    }

    override fun onStart() {
        super.onStart()
        initialize()
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
        btnLogin.setOnClickListener(loginHandler)
    }

    private fun doLogin(username: String, password: String) {
        ServiceManager.createSession(username, password)
        val call : Call<User> = ServiceManager.getUserService().getUser()
        call.enqueue(loginCallback)
    }

    private fun login(user : User) {
        val intent = Intent(this, ActivityMenu::class.java)
        userId = user.id
        userName = user.name
        userRole = user.role
        userCreatedDate = user.createdDate
        userSoldTicketsNo = user.soldTicketsNo
        userValidatedTicketsNo = user.validatedTicketsNo
        startActivity(intent)
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(USER_TEXT,etUsername.text.toString())
        outState?.putString(PASS_TEXT,etPassword.text.toString())
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        val lastEtUser = savedInstanceState?.getString(USER_TEXT)
        val lastEtPass = savedInstanceState?.getString(PASS_TEXT)
        etUsername.setText(lastEtUser)
        etPassword.setText(lastEtPass)
    }

    companion object {
        private const val USER_TEXT = "userEtText"
        private const val PASS_TEXT = "passEtText"
    }
}
