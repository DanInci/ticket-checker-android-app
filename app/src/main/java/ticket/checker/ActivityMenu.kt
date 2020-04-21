package ticket.checker

import android.content.Intent
import android.os.Bundle
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.appbar.CollapsingToolbarLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ticket.checker.beans.OrganizationMember
import ticket.checker.beans.OrganizationMemberWithPretendedRole
import ticket.checker.extras.OrganizationRole
import ticket.checker.extras.Util
import ticket.checker.extras.Util.DATE_FORMAT_MONTH_NAME
import ticket.checker.services.ServiceManager
import java.util.*

class ActivityMenu : AppCompatActivity(), View.OnClickListener {

    private lateinit var organizationId: UUID
    private lateinit var organizationName: String
    private lateinit var organizationRole: OrganizationRole

    private var headerHasLoaded = false

    private val collapsingToolbar by lazy {
        findViewById<CollapsingToolbarLayout>(R.id.collapsingToolbar)
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
            if (scrollRange == -1) {
                scrollRange = appBarLayout.totalScrollRange
            }
            if (scrollRange + verticalOffset == 0) {
                menuIsShown = true
                collapsingToolbar.title = if(AppTicketChecker.selectedOrganizationMembership != null) AppTicketChecker.selectedOrganizationMembership!!.organizationName else organizationName
                invalidateOptionsMenu()
            } else if (menuIsShown) {
                menuIsShown = false
                if (headerHasLoaded) {
                    collapsingToolbar.title = ""
                }
                invalidateOptionsMenu()
            }
        }
    }

    private val organizationMemberCallback: Callback<OrganizationMember> = object : Callback<OrganizationMember> {
        override fun onResponse(call: Call<OrganizationMember>, response: Response<OrganizationMember>) {
            if (response.isSuccessful) {
                val organizationMember = response.body() as OrganizationMember
                if(AppTicketChecker.selectedOrganizationMembership != null) {
                    AppTicketChecker.selectedOrganizationMembership =  organizationMember.withPretendedRole(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole)
                } else {
                    AppTicketChecker.selectedOrganizationMembership = organizationMember.withPretendedRole(organizationMember.role)
                }
                switchViews(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole)
                updateOrganizationMemberInfo(AppTicketChecker.selectedOrganizationMembership!!)
            } else {
                Util.treatBasicError(call, response, supportFragmentManager)
            }
        }

        override fun onFailure(call: Call<OrganizationMember>, t: Throwable?) {
            Util.treatBasicError(call, null, supportFragmentManager)
        }
    }

    private val tvName by lazy {
        findViewById<TextView>(R.id.name)
    }
    private val tvJoined by lazy {
        findViewById<TextView>(R.id.joined)
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
    private val cvControlPanel by lazy {
        findViewById<CardView>(R.id.controlPanel)
    }

    private var menuIsShown = false
    private var currentMenuItemId = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        this.organizationId = if(AppTicketChecker.selectedOrganizationMembership != null) AppTicketChecker.selectedOrganizationMembership!!.organizationId else intent.getSerializableExtra(ORGANIZATION_ID) as UUID
        this.organizationName = if(AppTicketChecker.selectedOrganizationMembership != null) AppTicketChecker.selectedOrganizationMembership!!.organizationName else intent.getStringExtra(ORGANIZATION_NAME)
        this.organizationRole = if(AppTicketChecker.selectedOrganizationMembership != null) AppTicketChecker.selectedOrganizationMembership!!.role else intent.getSerializableExtra(ORGANIZATION_ROLE) as OrganizationRole
        this.currentMenuItemId = savedInstanceState?.getInt(CURRENT_MENU_ITEM_ID) ?: -1

//        loadCollapsingToolbarImg()
        setSupportActionBar(toolbar)
        collapsingToolbar.title = organizationName
        appBarLayout.addOnOffsetChangedListener(appBarOffsetChangeListener)
        cvControlPanel.setOnClickListener(this)
        cvScan.setOnClickListener(this)
        cvStatistics.setOnClickListener(this)
    }

    override fun onStart() {
        super.onStart()
        if(AppTicketChecker.selectedOrganizationMembership != null) {
            switchViews(AppTicketChecker.selectedOrganizationMembership!!.pretendedRole)
            updateOrganizationMemberInfo(AppTicketChecker.selectedOrganizationMembership!!)
        } else {
            switchViews(organizationRole)
        }

        val call: Call<OrganizationMember> = ServiceManager.getOrganizationService().getMyOrganizationMembership(organizationId)
        call.enqueue(organizationMemberCallback)
    }

//    private fun loadCollapsingToolbarImg() {
//        val collapsingToolbarBackground = findViewById<ImageView>(R.id.bg_collapsingToolbar)
//        val baseUrl = if(AppTicketChecker.port != "") "http://${AppTicketChecker.address}:${AppTicketChecker.port}" else "http://${AppTicketChecker.address}"
//        Glide.with(applicationContext)
//                .asBitmap()
//                .load("$baseUrl/images/header.png")
//                .into(object : SimpleTarget<Bitmap>() {
//                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
//                        headerHasLoaded=true
//                        collapsingToolbar.title = " "
//                        collapsingToolbarBackground.setImageBitmap(resource)
//                        collapsingToolbarBackground.scaleType = ImageView.ScaleType.CENTER_CROP
//                    }
//                })
//    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if(AppTicketChecker.selectedOrganizationMembership != null) {
            when (AppTicketChecker.selectedOrganizationMembership!!.role) {
                OrganizationRole.OWNER, OrganizationRole.ADMIN -> menuInflater.inflate(R.menu.menu_admin, menu)
                OrganizationRole.PUBLISHER -> menuInflater.inflate(R.menu.menu_publisher, menu)
                OrganizationRole.VALIDATOR -> menuInflater.inflate(R.menu.menu_validator, menu)
                OrganizationRole.USER -> menuInflater.inflate(R.menu.menu_user, menu)
            }
        } else {
            when (organizationRole) {
                OrganizationRole.OWNER, OrganizationRole.ADMIN -> menuInflater.inflate(R.menu.menu_admin, menu)
                OrganizationRole.PUBLISHER -> menuInflater.inflate(R.menu.menu_publisher, menu)
                OrganizationRole.VALIDATOR -> menuInflater.inflate(R.menu.menu_validator, menu)
                OrganizationRole.USER -> menuInflater.inflate(R.menu.menu_user, menu)
            }
        }

        if (!menuIsShown) {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = false
            }
        } else {
            for (i in 0 until menu.size()) {
                menu.getItem(i).isVisible = true
            }
        }
        if(currentMenuItemId == -1) {
            menu.getItem(0).isChecked = true
        }
        else {
            checkMenuItem(currentMenuItemId)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        var validSelection = true
        when (item.itemId) {
            R.id.action_admin_mode -> {
                val pretendedRole = OrganizationRole.ADMIN
                switchViews(pretendedRole)
                tvCurrentRole.text = pretendedRole.name
                currentMenuItemId = R.id.action_admin_mode
                AppTicketChecker.selectedOrganizationMembership = AppTicketChecker.selectedOrganizationMembership?.copy(pretendedRole = pretendedRole)
            }
            R.id.action_publisher_mode -> {
                val pretendedRole = OrganizationRole.PUBLISHER
                switchViews(pretendedRole)
                tvCurrentRole.text = pretendedRole.name
                currentMenuItemId = R.id.action_publisher_mode
                AppTicketChecker.selectedOrganizationMembership = AppTicketChecker.selectedOrganizationMembership?.copy(pretendedRole = pretendedRole)
            }
            R.id.action_validator_mode -> {
                val pretendedRole = OrganizationRole.VALIDATOR
                switchViews(pretendedRole)
                tvCurrentRole.text = pretendedRole.name
                currentMenuItemId = R.id.action_validator_mode
                AppTicketChecker.selectedOrganizationMembership = AppTicketChecker.selectedOrganizationMembership?.copy(pretendedRole = pretendedRole)
            }
            R.id.action_user_mode -> {
                val pretendedRole = OrganizationRole.USER
                switchViews(pretendedRole)
                tvCurrentRole.text = pretendedRole.name
                currentMenuItemId = R.id.action_user_mode
                AppTicketChecker.selectedOrganizationMembership = AppTicketChecker.selectedOrganizationMembership?.copy(pretendedRole = pretendedRole)
            }
            R.id.action_select_organization -> toSelectOrganization()
            R.id.action_my_profile -> toMyProfile()
            R.id.action_logout -> logout()
            else -> {
                validSelection = false
            }
        }
        if (validSelection) {
            checkMenuItem(item.itemId)
        }
        return validSelection
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.controlPanel -> {
//                val intent = Intent(this, ActivityControlPanel::class.java)
//                startActivity(intent)
            }
            R.id.scan -> {
                val intent = Intent(this, ActivityScan::class.java)
                startActivity(intent)
            }
            R.id.statistics -> {
                val intent = Intent(this, ActivityStatistics::class.java)
                startActivity(intent)
            }
        }
    }

    private fun switchViews(role: OrganizationRole) {
        when (role) {
            OrganizationRole.OWNER, OrganizationRole.ADMIN -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Administration area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your administrator priveleges to add, delete tickets or see user information"
            }
            OrganizationRole.PUBLISHER -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Publisher area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your publisher priveleges to add tickets or see information about them"
            }
            OrganizationRole.VALIDATOR -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.VISIBLE
                findViewById<TextView>(R.id.controlPanelTitle).text = "Validator area"
                findViewById<TextView>(R.id.controlPanelDescription).text = "Use your validator priveleges to validate/invalidate tickets or see information about them"
            }
            OrganizationRole.USER -> {
                cvScan.visibility = View.VISIBLE
                cvStatistics.visibility = View.VISIBLE
                cvControlPanel.visibility = View.GONE
            }
        }
    }

    private fun updateOrganizationMemberInfo(member: OrganizationMemberWithPretendedRole) {
        findViewById<ProgressBar>(R.id.lsName).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsJoined).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsHighestRole).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsCurrentRole).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsCreatedTickets).visibility = View.GONE
        findViewById<ProgressBar>(R.id.lsValidatedTickets).visibility = View.GONE

        tvName.text = member.name
        tvJoined.text = DATE_FORMAT_MONTH_NAME.format(member.joinedAt)
        tvHighestRole.text = member.role.role
        tvCurrentRole.text = member.pretendedRole.role
        tvCreatedTickets.text = member.soldTicketsNo.toString()
        tvValidatedTickets.text = member.validatedTicketsNo.toString()
    }

    private fun checkMenuItem(menuItemId: Int) {
        val menu = toolbar.menu
        if (menuItemId != R.id.action_logout) {
            (0 until (menu.size() - 1))
                    .map { menu.getItem(it) }
                    .forEach { it.isChecked = it.itemId == menuItemId }
        }
    }

    private fun toSelectOrganization() {
        AppTicketChecker.selectedOrganizationMembership = null
        finish()
    }

    private fun toMyProfile() {
        val intent  = Intent(this@ActivityMenu, ActivityProfile::class.java)
        intent.putExtra(ActivityProfile.USER_ID, AppTicketChecker.loggedInUser!!.id)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }

    private fun logout() {
        AppTicketChecker.clearSession()
        val intent = Intent(this, ActivityLogin::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(CURRENT_MENU_ITEM_ID, currentMenuItemId)
    }

    companion object {
        const val ORGANIZATION_ID = "organizationId"
        const val ORGANIZATION_NAME = "organizationName"
        const val ORGANIZATION_ROLE = "organizationRole"
        private const val CURRENT_MENU_ITEM_ID = "currentMenuItemId"
    }

}
