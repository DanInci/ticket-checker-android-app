package ticket.checker.dialogs

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.core.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.*
import ticket.checker.R
import ticket.checker.listeners.DialogExitListener
import ticket.checker.listeners.DialogResponseListener
import ticket.checker.services.ServiceManager


class DialogInfo : DialogFragment(), View.OnClickListener {

    private var title: String? = null
    private var desc: String? = null
    private var dialogType: DialogType? = null

    private var header : RelativeLayout? = null
    private var btnClose : ImageButton? = null
    private var tvTitle : TextView? = null
    private var loadingSpinner : ProgressBar? = null
    private var ivDialogIcon: ImageView? = null
    private var tvDesc : TextView? = null
    private var yesNoButtons : LinearLayout? = null
    private var btnYes : Button? = null
    private var btnNo : Button? = null
    private var btnToLogin : Button? = null

    var dialogResponseListener : DialogResponseListener? = null
    var dialogExitListener : DialogExitListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            title = arguments?.getString(DIALOG_TITLE)
            desc = arguments?.getString(DIALOG_MSG)
            dialogType = DialogType.valueOf(arguments?.getString(DIALOG_TYPE)!!)
            if(dialogType == DialogType.AUTH_ERROR) {
                ServiceManager.invalidateSession()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_info, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        header = view.findViewById(R.id.dialogHeader)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose?.setOnClickListener(this)
        tvTitle = view.findViewById(R.id.tvTitle)
        tvTitle?.text = title
        loadingSpinner = view.findViewById(R.id.loadingSpinner)
        ivDialogIcon = view.findViewById(R.id.ivDialogIcon)
        tvDesc = view.findViewById(R.id.tvDescription)
        tvDesc?.text = desc
        yesNoButtons = view.findViewById(R.id.yesNoButtons)
        btnYes = view.findViewById(R.id.btnYes)
        btnYes?.setOnClickListener(this)
        btnNo = view.findViewById(R.id.btnNo)
        btnNo?.setOnClickListener(this)
        btnToLogin = view.findViewById(R.id.btnToLogin)
        btnToLogin?.setOnClickListener(this)
        switchViews()
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnClose -> {
                dismiss()
            }
            R.id.btnToLogin -> {
                val i = context?.packageManager?.getLaunchIntentForPackage(context!!.packageName)
                i?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(i)
            }
            R.id.btnYes -> {
                dismiss()
                dialogResponseListener?.onResponse(true)
            }
            R.id.btnNo -> {
                dismiss()
                dialogResponseListener?.onResponse(false)
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        dialogExitListener?.onItemRemoved()
    }

//    fun showHeader(showHeader : Boolean) {
//        if(showHeader) {
//            header?.visibility = View.VISIBLE
//        }
//        else {
//            header?.visibility = View.GONE
//        }
//    }

    private fun switchViews() {
        when(dialogType) {
            DialogType.LOADING -> {
                dialog?.setCancelable(false)
                dialog?.setCanceledOnTouchOutside(false)
                btnClose?.visibility = View.GONE
                yesNoButtons?.visibility = View.GONE
                btnToLogin?.visibility = View.GONE
                ivDialogIcon?.visibility = View.GONE
            }
            DialogType.SUCCESS -> {
                dialog?.setCancelable(true)
                dialog?.setCanceledOnTouchOutside(true)
                loadingSpinner?.visibility = View.GONE
                yesNoButtons?.visibility = View.GONE
                btnToLogin?.visibility = View.GONE
                tvDesc?.textAlignment = View.TEXT_ALIGNMENT_CENTER
                tvDesc?.setTextColor(ContextCompat.getColor(context!!,R.color.textBlack))
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_check_green, context?.theme))
                }
                else {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_check_green, context?.theme))
                }
            }
            DialogType.ERROR -> {
                dialog?.setCancelable(true)
                dialog?.setCanceledOnTouchOutside(true)
                loadingSpinner?.visibility = View.GONE
                yesNoButtons?.visibility = View.GONE
                btnToLogin?.visibility = View.GONE
                tvDesc?.textAlignment = View.TEXT_ALIGNMENT_CENTER
                tvDesc?.setTextColor(ContextCompat.getColor(context!!, R.color.materialYellow))
            }
            DialogType.AUTH_ERROR -> {
                dialog?.setCancelable(false)
                dialog?.setCanceledOnTouchOutside(false)
                btnClose?.visibility = View.GONE
                loadingSpinner?.visibility = View.GONE
                yesNoButtons?.visibility = View.GONE
                tvDesc?.textAlignment = View.TEXT_ALIGNMENT_CENTER
                tvDesc?.setTextColor(ContextCompat.getColor(context!!, R.color.materialYellow))
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_no_persons, context?.theme))
                }
                else {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_no_persons, context?.theme))
                }
            }
            DialogType.YES_NO -> {
                dialog?.setCancelable(true)
                dialog?.setCanceledOnTouchOutside(true)
                loadingSpinner?.visibility = View.GONE
                btnToLogin?.visibility = View.GONE
                tvDesc?.textAlignment = View.TEXT_ALIGNMENT_CENTER
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_question_mark, context?.theme))
                }
                else {
                    ivDialogIcon?.setImageDrawable(resources.getDrawable(R.drawable.ic_question_mark, context?.theme))
                }
            }
        }
    }

    companion object {
        private const val DIALOG_TITLE = "title"
        private const val DIALOG_MSG = "msg"
        private const val DIALOG_TYPE = "dialogType"

        fun newInstance(title: String, msg: String, dialogType : DialogType): DialogInfo {
            val fragment = DialogInfo()
            val args = Bundle()
            args.putString(DIALOG_TITLE, title)
            args.putString(DIALOG_MSG, msg)
            args.putString(DIALOG_TYPE, dialogType.name)
            fragment.arguments = args
            return fragment
        }
    }
}
