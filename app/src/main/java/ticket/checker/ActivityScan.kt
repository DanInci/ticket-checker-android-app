package ticket.checker

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import ticket.checker.camera.CameraSource
import ticket.checker.camera.CameraSurfaceView
import ticket.checker.camera.FocusView
import ticket.checker.dialogs.DialogBarcodeConfig
import ticket.checker.dialogs.DialogScan
import ticket.checker.extras.BarcodeType
import ticket.checker.listeners.BarcodeTypeChangeListener
import ticket.checker.listeners.IScanDialogListener


class ActivityScan : AppCompatActivity(), View.OnClickListener, BarcodeTypeChangeListener {

    private val cameraSource by lazy {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        CameraSource.Builder(this)
                .setRequestedPreviewSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedFps(15.0f)
                .build()
    }

    private val cameraSurfaceView: CameraSurfaceView by lazy {
        findViewById<CameraSurfaceView>(R.id.cameraSurfaceView)
    }
    private val focusView : FocusView by lazy {
        findViewById<FocusView>(R.id.focusView)
    }
    private val btnBack by lazy {
        findViewById<ImageView>(R.id.btnBack)
    }
    private val btnFlash by lazy {
        findViewById<ImageButton>(R.id.btnFlash)
    }
    private val btnConfig by lazy {
        findViewById<ImageButton>(R.id.btnConfig)
    }

    private var currentBarcodeType = BarcodeType.ALL_FORMATS
    private var isTorchOn = false

    private val barcodeProcessor = object : Detector.Processor<Barcode> {
        override fun release() {}
        override fun receiveDetections(detections: Detector.Detections<Barcode>) {
            val qrCodes: SparseArray<Barcode> = detections.detectedItems
            if (qrCodes.size() != 0) {
                cameraSource.setDetectionActive(false)
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(300)
                val code = qrCodes.get(qrCodes.keyAt(0))
                val dialogScan = DialogScan.newInstance(code.rawValue)
                dialogScan.scanDialogListener = scanDialogListener
                dialogScan.show(supportFragmentManager, "DIALOG_SCAN")
            }
        }
    }
    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {
            if (ActivityCompat.checkSelfPermission(this@ActivityScan, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this@ActivityScan, arrayOf(android.Manifest.permission.CAMERA), ActivityScan.CAMERA_PERMISSION)
                return
            }
            cameraSource.start(cameraSurfaceView.holder)
            cameraSource.setDetectionActive(true)
            if(isTorchOn) {
                toggleFlash(true)
            }
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            if (ActivityCompat.checkSelfPermission(this@ActivityScan, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                 return
            }

            if (isTorchOn) {
                toggleFlash(false)
            }
            cameraSource.setDetectionActive(false)
            cameraSource.stop()
        }
    }
    private val scanDialogListener = object : IScanDialogListener {
        override fun dismiss() {
            cameraSource.setDetectionActive(true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        currentBarcodeType = getSavedBarcodeType()
        val barcodeDetector = getBarcodeDetector(currentBarcodeType)
        cameraSource.setDetector(barcodeDetector)

        cameraSurfaceView.cameraSource = cameraSource
        cameraSurfaceView.focusView = focusView
        cameraSurfaceView.holder.addCallback(surfaceHolderCallback)

        btnBack.setOnClickListener(this)
        btnFlash.setOnClickListener(this)
        btnConfig.setOnClickListener(this)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    cameraSource.start(cameraSurfaceView.holder)
                    cameraSource.setDetectionActive(true)
                }
            }
        }
    }

    override fun onBarcodeTypeChanged(barcodeType: BarcodeType) {
        saveBarcodeType(barcodeType)
        currentBarcodeType = barcodeType
        val barcodeDetector = getBarcodeDetector(currentBarcodeType)
        cameraSource.setDetector(barcodeDetector)
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> finish()
            R.id.btnFlash -> {
                isTorchOn = toggleFlash(!isTorchOn)
            }
            R.id.btnConfig -> {
                val barcodeTypeConfigDialog = DialogBarcodeConfig.newInstance(currentBarcodeType)
                barcodeTypeConfigDialog.barcodeTypChangeListener = this
                barcodeTypeConfigDialog.show(supportFragmentManager, "DIALOG_BARCODE_CONFIG")
            }
        }
    }

    private fun toggleFlash(torchMode: Boolean) : Boolean {
        if(torchMode) {
            val successful = cameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH)
            if(successful) {
                btnFlash.setImageResource(R.drawable.ic_flashlight_on)
                return torchMode
            }
            else {
                Toast.makeText(baseContext, "Your device does not support back camera flash!", Toast.LENGTH_LONG).show()
            }
        }
        else {
            val successful = cameraSource.setFlashMode(Camera.Parameters.FLASH_MODE_OFF)
            if(successful) {
                btnFlash.setImageResource(R.drawable.ic_flashlight_off)
                return torchMode
            }
            else {
                Toast.makeText(baseContext, "Your device does not support back camera flash!", Toast.LENGTH_LONG).show()
            }
        }
        return !torchMode
    }

    private fun getBarcodeDetector(barcodeType: BarcodeType) : BarcodeDetector {
        val detector =  BarcodeDetector.Builder(this@ActivityScan).setBarcodeFormats(barcodeType.id).build()
        detector.setProcessor(barcodeProcessor)
        return detector
    }

    private fun getSavedBarcodeType() : BarcodeType {
        val pref = PreferenceManager.getDefaultSharedPreferences(AppTicketChecker.appContext)
        val barcodeTypeId = pref.getInt(CURRENT_BARCODE_TYPE, BarcodeType.ALL_FORMATS.id)
        return BarcodeType.fromIdToBarcodeType(barcodeTypeId)
    }

    private fun saveBarcodeType(barcodeType : BarcodeType) {
        val pref = PreferenceManager.getDefaultSharedPreferences(AppTicketChecker.appContext)
        val editor = pref.edit()
        editor.putInt(CURRENT_BARCODE_TYPE, barcodeType.id)
        editor.apply()
    }

    companion object {
        const val CAMERA_PERMISSION = 1001
        const val CURRENT_BARCODE_TYPE = "currentBarcodeType"
    }

}
