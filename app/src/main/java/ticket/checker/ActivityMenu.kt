package ticket.checker

import android.os.Bundle
import android.support.design.widget.AppBarLayout
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.activity_menu.*
import ticket.checker.extras.Constants.CURRENT_TOOLBAR_IMG
import ticket.checker.extras.Constants.DATE_FORMAT
import ticket.checker.extras.Constants.PRETENDED_USER_ROLE
import ticket.checker.extras.Constants.ROLE_ADMIN
import ticket.checker.extras.Constants.ROLE_USER
import ticket.checker.extras.Constants.SESSION_USER_CREATED_DATE
import ticket.checker.extras.Constants.SESSION_USER_ID
import ticket.checker.extras.Constants.SESSION_USER_NAME
import ticket.checker.extras.Constants.SESSION_USER_ROLE
import ticket.checker.service.ServiceManager
import java.util.*

class ActivityMenu : AppCompatActivity() {

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

    var menuIsShown = false

    private var userId : Long? = null
    private var userName : String? = null
    private var userRole : String? = null
    private var userCreatedDate : Date? = null

    private var pretendedUserRole: String? = null
    private var toolbarImg : Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        userId = intent.getLongExtra(SESSION_USER_ID,0L)
        userName = intent.getStringExtra(SESSION_USER_NAME)
        userRole = intent.getStringExtra(SESSION_USER_ROLE)
        pretendedUserRole = savedInstanceState?.getString(PRETENDED_USER_ROLE) ?: userRole
        toolbarImg = savedInstanceState?.getInt(CURRENT_TOOLBAR_IMG) ?: randomToolbarImg()
        val userCreatedDateMillis = intent.getLongExtra(SESSION_USER_CREATED_DATE,0L)
        userCreatedDateMillis?.let { userCreatedDate = Date(userCreatedDateMillis) }

        setContentView(R.layout.activity_menu)
        loadCollapsingToolbarImg()
        setSupportActionBar(toolbar)
        appBarLayout.addOnOffsetChangedListener(appBarOffsetChangeListener)
        switchViews()
        updateProfileInfo()
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
            else -> menuInflater.inflate(R.menu.menu_user, menu)
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
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when(item?.itemId) {
            R.id.action_admin_mode -> {
                pretendedUserRole = ROLE_ADMIN
                switchViews()
                updateProfileInfo()
            }
            R.id.action_user_mode -> {
                pretendedUserRole = ROLE_USER
                switchViews()
                updateProfileInfo()}
            R.id.action_logout -> logout()
            else -> { return false }
        }
        return true
    }

    private fun switchViews() {
        when(pretendedUserRole) {
            ROLE_ADMIN -> {
                actionScan.visibility = View.VISIBLE
                actionStatistics.visibility = View.VISIBLE
                actionAdmin.visibility = View.VISIBLE
            }
            else -> {
                actionScan.visibility = View.VISIBLE
                actionStatistics.visibility = View.VISIBLE
                actionAdmin.visibility = View.GONE
            }
        }
    }

    private fun updateProfileInfo() {
        tvName.text = userName
        tvCreated.text = DATE_FORMAT.format(userCreatedDate)
        tvHighestRole.text = userRole?.removePrefix("ROLE_")
        tvCurrentRole.text = pretendedUserRole?.removePrefix("ROLE_")
        tvCreatedTickets.text = "0" //TODO
        tvValidatedTickets.text = "0" //TODO
    }

    private fun logout() {
        ServiceManager.invalidateSession()
        finish()
    }

    override fun onBackPressed() {
        //DO nothing
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        outState?.putString(PRETENDED_USER_ROLE, pretendedUserRole)
        outState?.putInt(CURRENT_TOOLBAR_IMG, toolbarImg as Int)
    }

}
