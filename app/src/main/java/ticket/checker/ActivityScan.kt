package ticket.checker

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import ticket.checker.camera.CameraSource
import ticket.checker.camera.CameraSurfaceView
import ticket.checker.camera.FocusView
import ticket.checker.dialogs.DialogScan
import ticket.checker.listeners.IScanDialogListener


class ActivityScan : AppCompatActivity(), View.OnClickListener {

    private val cameraSource by lazy {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        CameraSource.Builder(this, barcodeDetector)
                .setRequestedPreviewSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedFps(15.0f)
                .build()
    }

    private val barcodeDetector by lazy {
        BarcodeDetector.Builder(this@ActivityScan).setBarcodeFormats(Barcode.ALL_FORMATS).build()
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
        findViewById<ImageView>(R.id.btnFlash)
    }

    private var isTorchOn = false

    private val barcodeProcessor = object : Detector.Processor<Barcode> {
        override fun release() {}
        override fun receiveDetections(detections: Detector.Detections<Barcode>) {
            val qrCodes: SparseArray<Barcode> = detections.detectedItems
            if (qrCodes.size() != 0) {
                stopBarcodeDetection()
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(300)
                val code = qrCodes.get(qrCodes.keyAt(0))
                val dialogScan = DialogScan.newInstance(code.rawValue)
                dialogScan.scanDialogListener = scanDialogListener
                dialogScan.show(supportFragmentManager, "DIALOG_SCAN")
            }
        }
    }
    private val scanDialogListener = object : IScanDialogListener {
        override fun dismiss() {
            startBarcodeDetection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        barcodeDetector.setProcessor(barcodeProcessor)
        cameraSurfaceView.holder.addCallback(cameraSource)
        cameraSurfaceView.cameraSource = cameraSource
        cameraSurfaceView.focusView = focusView

        btnBack.setOnClickListener(this)
        btnFlash.setOnClickListener(this)
    }

    override fun onResume() {
        super.onResume()
        startBarcodeDetection()
    }

    override fun onStop() {
        super.onStop()
        if (isTorchOn) {
            toggleFlash(false)
            stopBarcodeDetection()
        }
    }

    override fun onPause() {
        super.onPause()
        if (isTorchOn) {
            toggleFlash(false)
            stopBarcodeDetection()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            CAMERA_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBarcodeDetection()
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btnBack -> finish()
            R.id.btnFlash -> {
                isTorchOn = toggleFlash(!isTorchOn)
            }
        }
    }

    private fun toggleFlash(torchMode: Boolean) : Boolean {
        if(torchMode) {
            val successful = cameraSource.setFocusMode(Camera.Parameters.FLASH_MODE_TORCH)
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

    private fun stopBarcodeDetection() {
        cameraSource.setDetectionActive(false)
    }

    private fun startBarcodeDetection() {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this@ActivityScan, arrayOf(android.Manifest.permission.CAMERA), ActivityScan.CAMERA_PERMISSION)
            return
        }
        cameraSource.setDetectionActive(true)
    }

    companion object {
        const val CAMERA_PERMISSION = 1001
    }

}
