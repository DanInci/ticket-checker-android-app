package ticket.checker

import android.content.Context
import android.content.pm.PackageManager
import android.os.*
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import ticket.checker.dialogs.DialogScan
import ticket.checker.extras.Constants.PRETENDED_USER_ROLE
import ticket.checker.listeners.IScanDialogListener
import java.io.IOException

class ActivityScan : AppCompatActivity() {

    private val cameraPreview : SurfaceView by lazy {
        findViewById<SurfaceView>(R.id.cameraPreview)
    }
    private val barcodeDetector by lazy {
        BarcodeDetector.Builder(this@ActivityScan).setBarcodeFormats(Barcode.QR_CODE).build()
    }
    private val barcodeProcessor = object : Detector.Processor<Barcode> {
        override fun release() { }
        override fun receiveDetections(detections: Detector.Detections<Barcode>) {
            var qrCodes = detections.detectedItems
            if(qrCodes.size() != 0) {
                Handler(Looper.getMainLooper()).post { stopBarcodeDetection() }
                val vibrator = applicationContext.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(300)
                val dialogScan = DialogScan.newInstance(qrCodes.get(0).displayValue, pretendedUserRole)
                dialogScan.setDialogListenr(scanDialogListener)
                dialogScan.show(supportFragmentManager,"DIALOG_SCAN")
            }
        }

    }
    private val scanDialogListener = object : IScanDialogListener {
        override fun dismiss() {
            startBarcodeDetection(false)
        }
    }

    private val cameraSource by lazy {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        CameraSource.Builder(this,barcodeDetector)
                .setRequestedPreviewSize(displayMetrics.widthPixels,displayMetrics.heightPixels)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setRequestedFps(20.0f)
                .build()
    }
    private val cameraPreviewCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {
            startBarcodeDetection(true)
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            stopBarcodeDetection()
        }
    }
    private val requestCameraPermissionId = 1001
    private val pretendedUserRole by lazy {
        intent.getStringExtra(PRETENDED_USER_ROLE)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        cameraPreview.holder.addCallback(cameraPreviewCallback)
        barcodeDetector.setProcessor(barcodeProcessor)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode) {
            requestCameraPermissionId -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startBarcodeDetection(false)
                }
            }
        }
    }

    private fun startBarcodeDetection(askForPermission : Boolean) {
        if(ActivityCompat.checkSelfPermission(applicationContext,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if(askForPermission) {
                ActivityCompat.requestPermissions(this@ActivityScan, arrayOf(android.Manifest.permission.CAMERA), requestCameraPermissionId)
            }
            return
        }
        try {
            cameraSource.start(cameraPreview.holder)
        }
        catch(e : IOException) {
            e.printStackTrace()
        }
    }

    private fun stopBarcodeDetection() {
        cameraSource.stop()
    }
}