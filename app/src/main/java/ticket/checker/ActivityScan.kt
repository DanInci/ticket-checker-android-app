package ticket.checker

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Vibrator
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.DisplayMetrics
import android.util.SparseArray
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import ticket.checker.dialogs.DialogScan
import ticket.checker.camera.CameraSource
import ticket.checker.listeners.IScanDialogListener
import java.io.IOException


class ActivityScan : AppCompatActivity(), View.OnClickListener {

    private val cameraPreview: SurfaceView by lazy {
        findViewById<SurfaceView>(R.id.cameraPreview)
    }
    private var cameraSource: CameraSource? = null
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
                Handler(Looper.getMainLooper()).post { stopBarcodeDetection() }
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
            startBarcodeDetection(false)
        }
    }

    private val cameraPreviewCallback = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder?) {
            Handler(Looper.getMainLooper()).post { startBarcodeDetection(true) }
        }

        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {

        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            stopBarcodeDetection()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        2/0

        cameraPreview.holder.addCallback(cameraPreviewCallback)
        btnBack.setOnClickListener(this)
        btnFlash.setOnClickListener(this)
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
                    startBarcodeDetection(false)
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

    private fun startBarcodeDetection(askForPermission: Boolean) {
        if (ActivityCompat.checkSelfPermission(applicationContext, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            if (askForPermission) {
                ActivityCompat.requestPermissions(this@ActivityScan, arrayOf(android.Manifest.permission.CAMERA), CAMERA_PERMISSION)
            }
            return
        }
        try {
            cameraSource = createCameraSource()
            cameraSource?.start(cameraPreview.holder)
            if(isTorchOn) {
                toggleFlash(true)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun toggleFlash(torchMode: Boolean) : Boolean {
        if(cameraSource != null) {
            if(torchMode) {
                val successful = cameraSource?.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH) ?: false
                if(successful) {
                    btnFlash.setImageResource(R.drawable.ic_flashlight_on)
                    return torchMode
                }
                else {
                    Toast.makeText(baseContext, "Your device does not support back camera flash!", Toast.LENGTH_LONG).show()
                }
            }
            else {
                val successful = cameraSource?.setFlashMode(Camera.Parameters.FLASH_MODE_OFF) ?: false
                if(successful) {
                    btnFlash.setImageResource(R.drawable.ic_flashlight_off)
                    return torchMode
                }
                else {
                    Toast.makeText(baseContext, "Your device does not support back camera flash!", Toast.LENGTH_LONG).show()
                }
            }
        }
        return !torchMode
    }

    private fun stopBarcodeDetection() {
        cameraSource?.stop()
    }

    private fun createCameraSource(): CameraSource {
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        return CameraSource.Builder(this, createBarcodeDetector())
                .setRequestedPreviewSize(displayMetrics.widthPixels, displayMetrics.heightPixels)
                .setFacing(CameraSource.CAMERA_FACING_BACK)
                .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
                .setRequestedFps(15.0f)
                .build()
    }

    private fun createBarcodeDetector(): BarcodeDetector {
        val barcodeDetector = BarcodeDetector.Builder(this@ActivityScan).setBarcodeFormats(Barcode.ALL_FORMATS).build()
        barcodeDetector.setProcessor(barcodeProcessor)
        return barcodeDetector
    }

    companion object {
        const val CAMERA_PERMISSION = 1001
    }

}
