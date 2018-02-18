package ticket.checker

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.AppTicketChecker.Companion.userCreatedDate
import ticket.checker.AppTicketChecker.Companion.userId
import ticket.checker.AppTicketChecker.Companion.userName
import ticket.checker.AppTicketChecker.Companion.userRole
import ticket.checker.AppTicketChecker.Companion.userSoldTicketsNo
import ticket.checker.AppTicketChecker.Companion.userValidatedTicketsNo
import ticket.checker.beans.User
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT
import ticket.checker.extras.Util.ROLE_ADMIN
import ticket.checker.extras.Util.ROLE_USER
import ticket.checker.services.ServiceManager
import java.util.*

class ActivityMenu : AppCompatActivity(), View.OnClickListener {

    private val actionScan by lazy {
        findViewById<CardView>(R.id.scan)
    }
    private val actionStatistics by lazy {
        findViewById<CardView>(R.id.statistics)
    }
    private val actionAdmin by lazy {
        findViewById<CardView>(R.id.admin)
    }
    private val toolbar by lazy {
        findViewById<Toolbar>(R.id.toolbar)
    }
    private val appBarLayout by lazy {
        findViewById<AppBarLayout>(R.id.appBar)
    }
    private val appBarOffsetChangeListener = object : AppBarLayout.OnOffsetChangedListener {
        var scrollRange = -1

        override fun onOffsetChanged(appBarLayout: AppBarLayout, verticalOffset: Int) {
            if(scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if(scrollRange + verticalOffset == 0) {
                menuIsShown = true
                invalidateOptionsMenu()
            }
            else if(menuIsShown) {
                menuIsShown = false
                invalidateOptionsMenu()
            }
        }
    }
    private val updateUserInfoCallback : Callback<User> = object : Callback<User> {
        override fun onResponse(call: Call<User>, response: Response<User>) {
            if(response.isSuccessful) {
                val user = response.body()
                userId = user?.id
                userName = user?.name
                userCreatedDate = user?.createdDate
                userRole = user?.role
                userSoldTicketsNo = user?.soldTicketsNo
                userValidatedTicketsNo = user?.validatedTicketsNo
                if(!firstLoadHappen) {
                    pretendedUserRole = user?.role
                    firstLoadHappen = true
                    switchViews()
                }
                updateProfileInfo()
            }
            else {
                Util.treatBasicError(call, response, supportFragmentManager)
            }
        }
        override fun onFailure(call: Call<User>, t: Throwable?) {
            Util.treatBasicError(call,null, supportFragmentManager)
        }
    }
    private val tvName by lazy {
        findViewById<TextView>(R.id.name)
    }
    private val tvCreated by lazy {
        findViewById<TextView>(R.id.created)
    }
    private val tvHighestRole by lazy {
        findViewById<TextView>(R.id.highestRole)
    }
    private val tvCurrentRole by lazy {
        findViewById<TextView>(R.id.currentRole)
    }
    private val tvCreatedTickets by lazy {
        findViewById<TextView>(R.id.createdTickets)
    }
    private val tvValidatedTickets by lazy {
        findViewById<TextView>(R.id.validatedTickets)
    }
    private val cvScan by lazy {
        findViewById<CardView>(R.id.scan)
    }
    private val cvStatistics by lazy {
        findViewById<CardView>(R.id.statistics)
    }
    private val cvAdmin by lazy {
        findViewById<CardView>(R.id.admin)
    }

    var menuIsShown = false
    var firstLoadHappen = false

    private var pretendedUserRole: String? = null
    private var toolbarImg : Int? = null
    private var currentMenuItemId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        pretendedUserRole = savedInstanceState?.getString(PRETENDED_USER_ROLE) ?: if(userRole != null) { userRole } else { ROLE_USER }
        toolbarImg = savedInstanceState?.getInt(CURRENT_TOOLBAR_IMG) ?: randomToolbarImg()
        currentMenuItemId = savedInstanceState?.getInt(CURRENT_MENU_ITEM_ID) ?: R.id.action_admin_mode

        setContentView(R.layout.activity_menu)
        loadCollapsingToolbarImg()
        setSupportActionBar(toolbar)
        appBarLayout.addOnOffsetChangedListener(appBarOffsetChangeListener)
        cvAdmin.setOnClickListener(this)
        cvScan.setOnClickListener(this)
        cvStatistics.setOnClickListener(this)
        if(userId != null) {
            firstLoadHappen = true
            switchViews()
        }
    }

    override fun onStart() {
        super.onStart()
        updateProfileInfo()
        val call : Call<User> = ServiceManager.getUserService().getUser()
        call.enqueue(updateUserInfoCallback)
    }

    private fun randomToolbarImg() : Int {
        val destiny = Random().nextInt(3)
        return when(destiny) {
            0 -> R.drawable.bg_concert1
            1 -> R.drawable.bg_concert2
            else -> R.drawable.bg_concert3
        }
    }

    private fun loadCollapsingToolbarImg() {
        val collapsingToolbar = findViewById<ImageView>(R.id.bg_collapsingToolbar)
        Glide.with(this)
                .load(toolbarImg)
                .apply(RequestOptions().centerCrop())
                .into(collapsingToolbar)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        when(userRole) {
            ROLE_ADMIN -> menuInflater.inflate(R.menu.menu_admin, menu)
            ROLE_USER -> menuInflater.inflate(R.menu.menu_user, menu)
        }
        if(!menuIsShown) {
            for(i in 0 until menu.size()) {
                menu.getItem(i).isVisible=false
            }
        }
        else {
            for(i in 0 until menu.size()) {
                menu.getItem(i).isVisible=true
            }
        }
        checkMenuItem(currentMenuItemId)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var validSelection = true
        when(item.itemId) {
            R.id.action_admin_mode -> {
                pretendedUserRole = ROLE_ADMIN
                switchViews()
                updateProfileInfo()
                currentMenuItemId = R.id.action_admin_mode
            }
            R.id.action_user_mode -> {
                pretendedUserRole = ROLE_USER
                switchViews()
                updateProfileInfo()
                currentMenuItemId = R.id.action_user_mode
            }
            R.id.action_logout -> logout()
            else -> { validSelection = false }
        }
        if(validSelection) {
            checkMenuItem(item.itemId)
        }
        return validSelection
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.admin ->{
                val intent = Intent(this, ActivityAdmin::class.java)
                startActivity(intent)
            }
            R.id.scan -> {
                val intent = Intent(this, ActivityScan::class.java)
                intent.putExtra(PRETENDED_USER_ROLE, pretendedUserRole)
                startActivity(intent)
            }
            R.id.statistics -> {
                val intent = Intent(this, ActivityStatistics::class.java)
                startActivity(intent)
            }
        }
    }

    private fun switchViews() {
        when(pretendedUserRole) {
            ROLE_ADMIN -> {
                actionScan.visibility = View.VISIBLE
                actionStatistics.visibility = View.VISIBLE
                actionAdmin.visibility = View.VISIBLE
            }
            ROLE_USER -> {
                actionScan.visibility = View.VISIBLE
                actionStatistics.visibility = View.VISIBLE
                actionAdmin.visibility = View.GONE
            }
        }
    }

    private fun updateProfileInfo() {
        if(firstLoadHappen) {
            findViewById<ProgressBar>(R.id.lsName).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCreated).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsHighestRole).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCurrentRole).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsCreatedTickets).visibility = View.GONE
            findViewById<ProgressBar>(R.id.lsValidatedTickets).visibility = View.GONE

            tvName.text = userName
            tvCreated.text = DATE_FORMAT.format(userCreatedDate)
            tvHighestRole.text = userRole?.removePrefix("ROLE_")
            tvCurrentRole.text = pretendedUserRole?.removePrefix("ROLE_")
            tvCreatedTickets.text = userSoldTicketsNo.toString()
            tvValidatedTickets.text = userValidatedTicketsNo.toString()
        }
    }

    private fun checkMenuItem(menuItemId : Int) {
        val menu = toolbar.menu
        if(menuItemId != R.id.action_logout) {
            (0 until (menu.size() -1))
                    .map{ menu.getItem(it) }
                    .forEach{it.isChecked = it.itemId == menuItemId }
        }
    }

    private fun logout() {
        AppTicketChecker.clearSession()
        finish()
    }

    override fun onBackPressed() {
        //DO nothing
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(PRETENDED_USER_ROLE, pretendedUserRole)
        outState.putInt(CURRENT_TOOLBAR_IMG, toolbarImg as Int)
        outState.putInt(CURRENT_MENU_ITEM_ID, currentMenuItemId)
    }

    companion object {
        const val PRETENDED_USER_ROLE = "pretendedUserRole"
        private const val CURRENT_TOOLBAR_IMG = "currentToolbarImg"
        private const val CURRENT_MENU_ITEM_ID = "currentMenuItemId"
    }

}
