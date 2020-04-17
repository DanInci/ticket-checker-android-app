package ticket.checker.dialogs

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.Spinner
import ticket.checker.R
import ticket.checker.extras.BarcodeType
import ticket.checker.listeners.BarcodeTypeChangeListener


class DialogBarcodeConfig : DialogFragment(), View.OnClickListener {

    var barcodeTypChangeListener : BarcodeTypeChangeListener? = null

    lateinit var btnClose : ImageButton
    lateinit var spinnerBarcodeType : Spinner
    lateinit var btnSave : Button

    lateinit var currentBarcodeType : BarcodeType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if(arguments != null) {
            currentBarcodeType = (arguments?.getSerializable(CURRENT_BARCODE_TYPE) as BarcodeType?) ?: BarcodeType.ALL_FORMATS
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.dialog_barcode_config, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog?.window?.requestFeature(Window.FEATURE_NO_TITLE)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        btnClose = view.findViewById(R.id.btnClose)
        btnClose.setOnClickListener(this)
        spinnerBarcodeType = view.findViewById(R.id.spinnerBarcodeType)
        spinnerBarcodeType.isClickable = true
        spinnerBarcodeType.isFocusable = true
        spinnerBarcodeType.adapter = ArrayAdapter<String>(view.context, R.layout.spinner_item,  BarcodeType.values().map { it -> it.format })
        spinnerBarcodeType.setSelection(currentBarcodeType.ordinal)
        adjustDropDownHeight(spinnerBarcodeType)
        btnSave = view.findViewById(R.id.btnSave)
        btnSave.setOnClickListener(this)
    }

    override fun onClick(v: View) {
        when(v.id) {
            R.id.btnClose -> dismiss()
            R.id.btnSave -> {
                val barcodeFormat = spinnerBarcodeType.selectedItem as String
                val newBarcodeType = BarcodeType.fromFormatToBarcodeType(barcodeFormat)
                if(newBarcodeType != currentBarcodeType) {
                    barcodeTypChangeListener?.onBarcodeTypeChanged(newBarcodeType)
                }
                dismiss()
            }
        }
    }

    private fun adjustDropDownHeight(spinner: Spinner) {
        val popup = Spinner::class.java.getDeclaredField("mPopup")
        popup.isAccessible = true
        val popupWindow = popup.get(spinner) as android.widget.ListPopupWindow
        popupWindow.height = 300
    }

    companion object {
        private const val CURRENT_BARCODE_TYPE = "currentBarcodeType"

        fun newInstance(currentBarcodeFormat: BarcodeType): DialogBarcodeConfig {
            val fragment = DialogBarcodeConfig()
            val args = Bundle()
            args.putSerializable(CURRENT_BARCODE_TYPE, currentBarcodeFormat)
            fragment.arguments = args
            return fragment
        }
    }
}