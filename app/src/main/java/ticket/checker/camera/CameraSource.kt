package ticket.checker.camera

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.ImageFormat
import android.graphics.Rect
import android.hardware.Camera
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.WindowManager
import com.google.android.gms.common.images.Size
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.Frame
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*


class CameraSource {
    private lateinit var mContext: Context
    var mFacing = CAMERA_FACING_BACK

    private val mCameraLock = Any()
    private lateinit var mCamera: Camera

    var mRotation: Int = 0

    private lateinit var mPreviewSize: Size

    var mRequestedFps = 30.0f
    var mRequestedPreviewWidth = 1024
    var mRequestedPreviewHeight = 768


    var mFocusMode: String = Camera.Parameters.FOCUS_MODE_AUTO
    var mFlashMode: String = Camera.Parameters.FLASH_MODE_OFF

    private val mBytesToByteBuffer = HashMap<ByteArray, ByteBuffer>()

    /**
     * Dedicated thread and associated runnable for calling into the detector with frames, as the
     * frames become available from the camera.
     */
    private lateinit var mProcessingThread: Thread
    private lateinit var mFrameProcessor: FrameProcessingRunnable

    /**
     * Opens the camera and starts sending preview frames to the underlying detector.  The supplied
     * surface holder is used for the preview so frames can be displayed to the user.
     *
     * @param surfaceHolder the surface holder to use for the preview frames
     * @throws IOException if the supplied surface holder could not be used as the preview display
     */
    fun start(surfaceHolder: SurfaceHolder): CameraSource {
        synchronized(mCameraLock) {
            mCamera = createCamera()
            mCamera.setPreviewDisplay(surfaceHolder)
            mCamera.startPreview()

            mProcessingThread = Thread(mFrameProcessor)
            mFrameProcessor.setActive(true)
            mProcessingThread.start()
        }
        return this
    }

    fun setDetector(mDetector: Detector<*>?) {
        mFrameProcessor.changeDetector(mDetector)
    }

    fun setDetectionActive(active : Boolean) {
        mFrameProcessor.setDetectActive(active)
    }

    /**
     * Stops the camera and releases the resources of the camera and underlying detector.
     */
    fun release() {
        synchronized(mCameraLock) {
            stop()
            mFrameProcessor.release()
        }
    }

    /**
     * Closes the camera and stops sending frames to the underlying frame detector.
     *
     *
     * This camera source may be restarted again by calling [.start] or
     * [.start].
     *
     *
     * Call [.release] instead to completely shut down this camera source and release the
     * resources of the underlying detector.
     */
    fun stop() {
        synchronized(mCameraLock) {
            mFrameProcessor.setActive(false)
           try {
               // Wait for the thread to complete to ensure that we can't have multiple threads
               // executing at the same time (i.e., which would happen if we called start too
               // quickly after stop).

               mProcessingThread.join()
           } catch (e: InterruptedException) {
               Log.d(TAG, "Frame processing thread interrupted on release.")
           }


            mCamera.stopPreview()
            mCamera.setPreviewCallbackWithBuffer(null)
            try {
                // We want to be compatible back to Gingerbread, but SurfaceTexture
                // wasn't introduced until Honeycomb.  Since the interface cannot use a SurfaceTexture, if the
                // developer wants to display a preview we must use a SurfaceHolder.  If the developer doesn't
                // want to display a preview we use a SurfaceTexture if we are running at least Honeycomb.

                mCamera.setPreviewDisplay(null)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear camera preview: $e")
            }

            mCamera.release()
        }
    }

    fun doZoom(scale: Float): Int {
        synchronized(mCameraLock) {
            var currentZoom = 0
            val maxZoom: Int
            val parameters = mCamera.parameters
            if (!parameters.isZoomSupported) {
                Log.w(TAG, "Zoom is not supported on this device")
                return currentZoom
            }
            maxZoom = parameters.maxZoom

            currentZoom = parameters.zoom + 1
            val newZoom: Float
            newZoom = if (scale > 1) {
                currentZoom + scale * (maxZoom / 10)
            } else {
                currentZoom * scale
            }
            currentZoom = Math.round(newZoom) - 1
            if (currentZoom < 0) {
                currentZoom = 0
            } else if (currentZoom > maxZoom) {
                currentZoom = maxZoom
            }
            parameters.zoom = currentZoom
            mCamera.parameters = parameters
            return currentZoom
        }
    }

    /**
     * Initiates taking a picture, which happens asynchronously.  The camera source should have been
     * activated previously with [.start] or [.start].  The camera
     * preview is suspended while the picture is being taken, but will resume once picture taking is
     * done.
     *
     * @param shutter the callback for image capture moment, or null
     * @param jpeg    the callback for JPEG image data, or null
     */
    fun takePicture(shutter: ShutterCallback, jpeg: PictureCallback) {
        synchronized(mCameraLock) {
            val startCallback = PictureStartCallback()
            startCallback.mDelegate = shutter
            val doneCallback = PictureDoneCallback()
            doneCallback.mDelegate = jpeg
            mCamera.takePicture(startCallback, null, null, doneCallback)
        }
    }

    /**
     * Sets the focus mode.
     *
     * @param mode the focus mode
     * @return `true` if the focus mode is set, `false` otherwise
     * @see .getFocusMode
     */
    fun setFocusMode(mode: String?): Boolean {
        synchronized(mCameraLock) {
            if (mode != null) {
                val parameters = mCamera.parameters
                if (parameters.supportedFocusModes.contains(mode)) {
                    parameters.focusMode = mode
                    mCamera.parameters = parameters
                    mFocusMode = mode
                    return true
                }
            }
            return false
        }
    }

    /**
     * Sets the flash mode.
     *
     * @param mode flash mode.
     * @return `true` if the flash mode is set, `false` otherwise
     * @see .getFlashMode
     */
    fun setFlashMode(mode: String?): Boolean {
        synchronized(mCameraLock) {
            if (mode != null) {
                val parameters = mCamera.parameters
                if (parameters.supportedFlashModes != null && parameters.supportedFlashModes.contains(mode)) {
                    parameters.flashMode = mode
                    mCamera.parameters = parameters
                    mFlashMode = mode
                    return true
                }
            }
            return false
        }
    }

    /**
     * Starts camera auto-focus and registers a callback function to run when
     * the camera is focused.  This method is only valid when preview is active
     * (between [.start] or [.start] and before [.stop] or [.release]).
     *
     *
     *
     * Callers should check
     * [.getFocusMode] to determine if
     * this method should be called. If the camera does not support auto-focus,
     * it is a no-op and [AutoFocusCallback.onAutoFocus]
     * callback will be called immediately.
     *
     *
     *
     * If the current flash mode is not
     * [Camera.Parameters.FLASH_MODE_OFF], flash may be
     * fired during auto-focus, depending on the driver and camera hardware.
     *
     *
     *
     * @param cb the callback to run
     * @see .cancelAutoFocus
     */
    fun autoFocus(cb: AutoFocusCallback) {
        synchronized(mCameraLock) {
            val autoFocusCallback = CameraAutoFocusCallback()
            autoFocusCallback.mDelegate = cb
            mCamera.autoFocus(autoFocusCallback)
        }
    }

    /**
     * Cancels any auto-focus function in progress.
     * Whether or not auto-focus is currently in progress,
     * this function will return the focus position to the default.
     * If the camera does not support auto-focus, this is a no-op.
     *
     * @see .autoFocus
     */
    fun cancelAutoFocus() {
        synchronized(mCameraLock) {
            mCamera.cancelAutoFocus()
        }
    }

    /**
     * Called from CameraSurfaceView to set touch focus.
     *
     * @param - Rect - new area for auto focus
     */
    fun doTouchFocus(tfocusRect: Rect) {
        try {
            mCamera.cancelAutoFocus()

            val focusList = ArrayList<Camera.Area>()
            val focusArea = Camera.Area(tfocusRect, 1000)
            focusList.add(focusArea)

            val para = mCamera.parameters
            para.focusAreas = focusList
            para.meteringAreas = focusList
            mCamera.parameters = para

            mCamera.autoFocus { _, camera -> camera.cancelAutoFocus() }
        } catch (e: Exception) {
            Log.i(TAG, "Unable to auto focus on touch")
        }

    }

    /**
     * Sets camera auto-focus move callback.
     *
     * @param cb the callback to run
     * @return `true` if the operation is supported (i.e. from Jelly Bean), `false` otherwise
     */
    fun setAutoFocusMoveCallback(cb: AutoFocusMoveCallback): Boolean {
        synchronized(mCameraLock) {
            val autoFocusMoveCallback = CameraAutoFocusMoveCallback()
            autoFocusMoveCallback.mDelegate = cb
            mCamera.setAutoFocusMoveCallback(autoFocusMoveCallback)
        }

        return true
    }

    /**
     * Opens the camera and applies the user settings.
     *
     * @throws RuntimeException if the method fails
     */
    private fun createCamera(): Camera {
        val requestedCameraId = getIdForRequestedCamera(mFacing)
        if (requestedCameraId == -1) throw RuntimeException("Could not find requested camera.")

        val camera = Camera.open(requestedCameraId)

        val sizePair = selectSizePair(camera, mRequestedPreviewWidth, mRequestedPreviewHeight) ?: throw RuntimeException("Could not find suitable preview size.")
        val pictureSize = sizePair.pictureSize()
        mPreviewSize = sizePair.previewSize()

        val previewFpsRange = selectPreviewFpsRange(camera, mRequestedFps)

        val parameters = camera.parameters

        if (pictureSize != null) {
            parameters.setPictureSize(pictureSize.width, pictureSize.height)
        }

        parameters.setPreviewSize(mPreviewSize.width, mPreviewSize.height)
        parameters.setPreviewFpsRange(
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MIN_INDEX],
                previewFpsRange[Camera.Parameters.PREVIEW_FPS_MAX_INDEX])
        parameters.previewFormat = ImageFormat.NV21

        setRotation(camera, parameters, requestedCameraId)

        if (parameters.supportedFocusModes.contains(
                        mFocusMode)) {
            parameters.focusMode = mFocusMode
        } else {
            Log.i(TAG, "Camera focus mode: $mFocusMode is not supported on this device.")
        }

        if (parameters.supportedFlashModes != null) {
            if (parameters.supportedFlashModes.contains(
                            mFlashMode)) {
                parameters.flashMode = mFlashMode
            } else {
                Log.i(TAG, "Camera flash mode: $mFlashMode is not supported on this device.")
            }
        }

        camera.parameters = parameters

        // Four frame buffers are needed for working with the camera:
        //
        //   one for the frame that is currently being executed upon in doing detection
        //   one for the next pending frame to process immediately upon completing detection
        //   two for the frames that the camera uses to populate future preview images
        camera.setPreviewCallbackWithBuffer(CameraPreviewCallback())
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize))
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize))
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize))
        camera.addCallbackBuffer(createPreviewBuffer(mPreviewSize))

        return camera
    }

    /**
     * Gets the id for the camera specified by the direction it is facing.  Returns -1 if no such
     * camera was found.
     *
     * @param facing the desired camera (front-facing or rear-facing)
     */
    private fun getIdForRequestedCamera(facing: Int): Int {
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until Camera.getNumberOfCameras()) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == facing) {
                return i
            }
        }
        return -1
    }

    /**
     * Selects the most suitable preview and picture size, given the desired width and height.
     *
     *
     * Even though we may only need the preview size, it's necessary to find both the preview
     * size and the picture size of the camera together, because these need to have the same aspect
     * ratio.  On some hardware, if you would only set the preview size, you will get a distorted
     * image.
     *
     * @param camera        the camera to select a preview size from
     * @param desiredWidth  the desired width of the camera preview frames
     * @param desiredHeight the desired height of the camera preview frames
     * @return the selected preview and picture size pair
     */
    private fun selectSizePair(camera: Camera, desiredWidth: Int, desiredHeight: Int): SizePair? {
        val validPreviewSizes = generateValidPreviewSizeList(camera)

        // The method for selecting the best size is to minimize the sum of the differences between
        // the desired values and the actual values for width and height.  This is certainly not the
        // only way to select the best size, but it provides a decent tradeoff between using the
        // closest aspect ratio vs. using the closest pixel area.
        var selectedPair: SizePair? = null
        var minDiff = Integer.MAX_VALUE
        for (sizePair in validPreviewSizes) {
            val size = sizePair.previewSize()
            val diff = Math.abs(size.width - desiredWidth) + Math.abs(size.height - desiredHeight)
            if (diff < minDiff) {
                selectedPair = sizePair
                minDiff = diff
            }
        }

        return selectedPair
    }

    /**
     * Generates a list of acceptable preview sizes.  Preview sizes are not acceptable if there is
     * not a corresponding picture size of the same aspect ratio.  If there is a corresponding
     * picture size of the same aspect ratio, the picture size is paired up with the preview size.
     *
     *
     * This is necessary because even if we don't use still pictures, the still picture size must be
     * set to a size that is the same aspect ratio as the preview size we choose.  Otherwise, the
     * preview images may be distorted on some devices.
     */
    private fun generateValidPreviewSizeList(camera: Camera): List<SizePair> {
        val parameters = camera.parameters
        val supportedPreviewSizes = parameters.supportedPreviewSizes
        val supportedPictureSizes = parameters.supportedPictureSizes
        val validPreviewSizes = ArrayList<SizePair>()
        for (previewSize in supportedPreviewSizes) {
            val previewAspectRatio = previewSize.width.toFloat() / previewSize.height.toFloat()

            // By looping through the picture sizes in order, we favor the higher resolutions.
            // We choose the highest resolution in order to support taking the full resolution
            // picture later.
            for (pictureSize in supportedPictureSizes) {
                val pictureAspectRatio = pictureSize.width.toFloat() / pictureSize.height.toFloat()
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(SizePair(previewSize, pictureSize))
                    break
                }
            }
        }

        // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
        // of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
        // still account for it.
        if (validPreviewSizes.size == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size")
            for (previewSize in supportedPreviewSizes) {
                // The null picture size will let us know that we shouldn't set a picture size.
                validPreviewSizes.add(SizePair(previewSize, null))
            }
        }

        return validPreviewSizes
    }

    /**
     * Selects the most suitable preview frames per second range, given the desired frames per
     * second.
     *
     * @param camera            the camera to select a frames per second range from
     * @param desiredPreviewFps the desired frames per second for the camera preview frames
     * @return the selected preview frames per second range
     */
    private fun selectPreviewFpsRange(camera: Camera, desiredPreviewFps: Float): IntArray {
        // The camera API uses integers scaled by a factor of 1000 instead of floating-point frame
        // rates.
        val desiredPreviewFpsScaled = (desiredPreviewFps * 1000.0f).toInt()

        // The method for selecting the best range is to minimize the sum of the differences between
        // the desired value and the upper and lower bounds of the range.  This may select a range
        // that the desired value is outside of, but this is often preferred.  For example, if the
        // desired frame rate is 29.97, the range (30, 30) is probably more desirable than the
        // range (15, 30).
        var selectedFpsRange: IntArray? = null
        var minDiff = Integer.MAX_VALUE
        val previewFpsRangeList = camera.parameters.supportedPreviewFpsRange
        for (range in previewFpsRangeList) {
            val deltaMin = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MIN_INDEX]
            val deltaMax = desiredPreviewFpsScaled - range[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]
            val diff = Math.abs(deltaMin) + Math.abs(deltaMax)
            if (diff < minDiff) {
                selectedFpsRange = range
                minDiff = diff
            }
        }
        return selectedFpsRange as IntArray
    }

    /**
     * Calculates the correct rotation for the given camera id and sets the rotation in the
     * parameters.  It also sets the camera's display orientation and rotation.
     *
     * @param parameters the camera parameters for which to set the rotation
     * @param cameraId   the camera id to set rotation based on
     */
    private fun setRotation(camera: Camera, parameters: Camera.Parameters, cameraId: Int) {
        val windowManager = mContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        var degrees = 0
        val rotation = windowManager.defaultDisplay.rotation
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> Log.e(TAG, "Bad rotation value: $rotation")
        }

        val cameraInfo = Camera.CameraInfo()
        Camera.getCameraInfo(cameraId, cameraInfo)

        val angle: Int
        val displayAngle: Int
        if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            angle = (cameraInfo.orientation + degrees) % 360
            displayAngle = (360 - angle) % 360 // compensate for it being mirrored
        } else {  // back-facing
            angle = (cameraInfo.orientation - degrees + 360) % 360
            displayAngle = angle
        }

        // This corresponds to the rotation constants in {@link Frame}.
        mRotation = angle / 90

        camera.setDisplayOrientation(displayAngle)
        parameters.setRotation(angle)
    }

    /**
     * Creates one buffer for the camera preview callback.  The size of the buffer is based off of
     * the camera preview size and the format of the camera image.
     *
     * @return a new preview buffer of the appropriate size for the current camera settings
     */
    private fun createPreviewBuffer(previewSize: Size): ByteArray {
        val bitsPerPixel = ImageFormat.getBitsPerPixel(ImageFormat.NV21)
        val sizeInBits = (previewSize.height * previewSize.width * bitsPerPixel).toLong()
        val bufferSize = Math.ceil(sizeInBits / 8.0).toInt() + 1

        //
        // NOTICE: This code only works when using play services v. 8.1 or higher.
        //

        // Creating the byte array this way and wrapping it, as opposed to using .allocate(),
        // should guarantee that there will be an array to work with.
        val byteArray = ByteArray(bufferSize)
        val buffer = ByteBuffer.wrap(byteArray)

        mBytesToByteBuffer[byteArray] = buffer
        return byteArray
    }

    companion object {
        const val CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK
        const val CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT

        private const val ASPECT_RATIO_TOLERANCE = 0.01f

        private const val TAG = "CameraSource"
    }

    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size.  To avoid distorted
     * preview images on some devices, the picture size must be set to a size that is the same
     * aspect ratio as the preview size or the preview may end up being distorted.  If the picture
     * size is null, then there is no picture size with the same aspect ratio as the preview size.
     */
    private class SizePair(previewSize: android.hardware.Camera.Size,
                           pictureSize: android.hardware.Camera.Size?) {
        private val mPreview: Size = Size(previewSize.width, previewSize.height)
        private var mPicture: Size? = null

        init {
            if (pictureSize != null) {
                mPicture = Size(pictureSize.width, pictureSize.height)
            }
        }

        fun previewSize(): Size {
            return mPreview
        }

        fun pictureSize(): Size? {
            return mPicture
        }
    }

    //==============================================================================================
    // Bridge Functionality for the Camera1 API
    //==============================================================================================

    /**
     * Callback interface used to signal the moment of actual image capture.
     */
    interface ShutterCallback {
        /**
         * Called as near as possible to the moment when a photo is captured from the sensor. This
         * is a good opportunity to play a shutter sound or give other feedback of camera operation.
         * This may be some time after the photo was triggered, but some time before the actual data
         * is available.
         */
        fun onShutter()
    }

    /**
     * Callback interface used to supply image data from a photo capture.
     */
    interface PictureCallback {
        /**
         * Called when image data is available after a picture is taken.  The format of the data
         * is a jpeg binary.
         */
        fun onPictureTaken(data: ByteArray)
    }

    /**
     * Callback interface used to notify on completion of camera auto focus.
     */
    interface AutoFocusCallback {
        /**
         * Called when the camera auto focus completes.  If the camera
         * does not support auto-focus and autoFocus is called,
         * onAutoFocus will be called immediately with a fake value of
         * `success` set to `true`.
         *
         *
         * The auto-focus routine does not lock auto-exposure and auto-white
         * balance after it completes.
         *
         * @param success true if focus was successful, false if otherwise
         */
        fun onAutoFocus(success: Boolean)
    }

    /**
     * Callback interface used to notify on auto focus start and stop.
     *
     *
     *
     * This is only supported in continuous autofocus modes -- [ ][Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO] and [ ][Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE]. Applications can show
     * autofocus animation based on this.
     */
    interface AutoFocusMoveCallback {
        /**
         * Called when the camera auto focus starts or stops.
         *
         * @param start true if focus starts to move, false if focus stops to move
         */
        fun onAutoFocusMoving(start: Boolean)
    }

    //==============================================================================================
    // Frame processing
    //==============================================================================================

    /**
     * Called when the camera has a new preview frame.
     */
    private inner class CameraPreviewCallback : Camera.PreviewCallback {
        override fun onPreviewFrame(data: ByteArray, camera: Camera) {
            mFrameProcessor.setNextFrame(data, camera)
        }
    }

    /**
     * This runnable controls access to the underlying receiver, calling it to process frames when
     * available from the camera.  This is designed to run detection on frames as fast as possible
     * (i.e., without unnecessary context switching or waiting on the next frame).
     *
     *
     * While detection is running on a frame, new frames may be received from the camera.  As these
     * frames come in, the most recent frame is held onto as pending.  As soon as detection and its
     * associated processing are done for the previous frame, detection on the mostly recently
     * received frame will immediately start on the same thread.
     */
    private inner class FrameProcessingRunnable : Runnable {
        private val mStartTimeMillis = SystemClock.elapsedRealtime()

        private var mDetector: Detector<*>? = null

        // This lock guards all of the member variables below.
        private val mLock = java.lang.Object()
        private var mActive = true

        private var mDetectActive = false

        // These pending variables hold the state associated with the new frame awaiting processing.
        private var mPendingTimeMillis: Long = 0
        private var mPendingFrameId = 0
        private var mPendingFrameData: ByteBuffer? = null

        /**
         * Releases the underlying receiver.  This is only safe to do after the associated thread
         * has completed, which is managed in camera source's release method above.
         */
        @SuppressLint("Assert")
        internal fun release() {
            assert(mProcessingThread.state == Thread.State.TERMINATED)
            mDetector?.release()
            mDetector = null
        }

        /**
         * Marks the runnable as active/not active.  Signals any blocked threads to continue.
         */
        internal fun setActive(active: Boolean) {
            synchronized(mLock) {
                mActive = active
                mLock.notifyAll()
            }
        }

        internal fun changeDetector(mDetector: Detector<*>?) {
            synchronized(mLock) {
                this.mDetector = mDetector
                mLock.notifyAll()
            }
        }

        internal fun setDetectActive(active : Boolean) {
            synchronized(mLock) {
                mDetectActive = active
                mLock.notifyAll()
            }
        }

        /**
         * Sets the frame data received from the camera.  This adds the previous unused frame buffer
         * (if present) back to the camera, and keeps a pending reference to the frame data for
         * future use.
         */
        internal fun setNextFrame(data: ByteArray, camera: Camera) {
            synchronized(mLock) {
                if (mPendingFrameData != null) {
                    camera.addCallbackBuffer(mPendingFrameData!!.array())
                    mPendingFrameData = null
                }

                if (!mBytesToByteBuffer.containsKey(data)) {
                    Log.d(TAG,"Skipping frame.  Could not find ByteBuffer associated with the image " + "data from the camera.")
                    return
                }

                // Timestamp and frame ID are maintained here, which will give downstream code some
                // idea of the timing of frames received and when frames were dropped along the way.
                mPendingTimeMillis = SystemClock.elapsedRealtime() - mStartTimeMillis
                mPendingFrameId++
                mPendingFrameData = mBytesToByteBuffer[data]

                // Notify the processor thread if it is waiting on the next frame (see below).
                mLock.notifyAll()
            }
        }

        /**
         * As long as the processing thread is active, this executes detection on frames
         * continuously.  The next pending frame is either immediately available or hasn't been
         * received yet.  Once it is available, we transfer the frame info to local variables and
         * run detection on that frame.  It immediately loops back for the next frame without
         * pausing.
         *
         *
         * If detection takes longer than the time in between new frames from the camera, this will
         * mean that this loop will run without ever waiting on a frame, avoiding any context
         * switching or frame acquisition time latency.
         *
         *
         * If you find that this is using more CPU than you'd like, you should probably decrease the
         * FPS setting above to allow for some idle time in between frames.
         */
        override fun run() {
            var outputFrame: Frame? = null
            var data: ByteBuffer? = null

            while (true) {
                synchronized(mLock) {
                    while (mActive && mPendingFrameData == null) {
                        try {
                            // Wait for the next frame to be received from the camera, since we
                            // don't have it yet.
                            mLock.wait()
                        } catch (e: InterruptedException) {
                            Log.d(TAG, "Frame processing loop terminated.", e)
                            return
                        }

                    }

                    if (!mActive) {
                        // Exit the loop once this camera source is stopped or released.  We check
                        // this here, immediately after the wait() above, to handle the case where
                        // setActive(false) had been called, triggering the termination of this
                        // loop.
                        return
                    }

                    outputFrame = Frame.Builder()
                            .setImageData(mPendingFrameData, mPreviewSize.width, mPreviewSize.height, ImageFormat.NV21)
                            .setId(mPendingFrameId)
                            .setTimestampMillis(mPendingTimeMillis)
                            .setRotation(mRotation)
                            .build()

                    // Hold onto the frame data locally, so that we can use this for detection
                    // below.  We need to clear mPendingFrameData to ensure that this buffer isn't
                    // recycled back to the camera before we are done using that data.
                    data = mPendingFrameData
                    mPendingFrameData = null
                }

                // The code below needs to run outside of synchronization, because this will allow
                // the camera to add pending frame(s) while we are running detection on the current
                // frame.

                try {
                    if(mDetectActive) {
                        mDetector?.receiveFrame(outputFrame)
                    }
                    else {
                        Log.i(TAG,"Detector is not active")
                    }
                } catch (t: Throwable) {
                    Log.e(TAG, "Exception thrown from receiver.", t)
                } finally {
                    mCamera.addCallbackBuffer(data?.array())
                }
            }
        }
    }

    //==============================================================================================
    // Private
    //==============================================================================================

    /**
     * Wraps the camera1 shutter callback so that the deprecated API isn't exposed.
     */
    private inner class PictureStartCallback : Camera.ShutterCallback {
        lateinit var mDelegate: ShutterCallback

        override fun onShutter() {
            mDelegate.onShutter()
        }
    }

    /**
     * Wraps the final callback in the camera sequence, so that we can automatically turn the camera
     * preview back on after the picture has been taken.
     */
    private inner class PictureDoneCallback : Camera.PictureCallback {
        lateinit var mDelegate: PictureCallback

        override fun onPictureTaken(data: ByteArray, camera: Camera) {
            mDelegate.onPictureTaken(data)
            synchronized(mCameraLock) {
                mCamera.startPreview()
            }
        }
    }

    /**
     * Wraps the camera1 auto focus callback so that the deprecated API isn't exposed.
     */
    private inner class CameraAutoFocusCallback : Camera.AutoFocusCallback {
        lateinit var mDelegate: AutoFocusCallback

        override fun onAutoFocus(success: Boolean, camera: Camera) {
            mDelegate.onAutoFocus(success)
        }
    }

    /**
     * Wraps the camera1 auto focus move callback so that the deprecated API isn't exposed.
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private inner class CameraAutoFocusMoveCallback : Camera.AutoFocusMoveCallback {
        lateinit var mDelegate: AutoFocusMoveCallback

        override fun onAutoFocusMoving(start: Boolean, camera: Camera) {
            mDelegate.onAutoFocusMoving(start)
        }
    }
    //==============================================================================================
    // Builder
    //==============================================================================================
    class Builder(context: Context?) {
        private val mCameraSource = CameraSource()

        init {
            if (context == null) {
                throw IllegalArgumentException("No context supplied.")
            }
            mCameraSource.mContext = context
        }

        /**
         * Sets the requested frame rate in frames per second.  If the exact requested value is not
         * not available, the best matching available value is selected.   Default: 30.
         */
        fun setRequestedFps(fps: Float): Builder {
            if (fps <= 0) {
                throw IllegalArgumentException("Invalid fps: $fps")
            }
            mCameraSource.mRequestedFps = fps
            return this
        }

        fun setFocusMode(mode: String): Builder {
            mCameraSource.mFocusMode = mode
            return this
        }

        fun setFlashMode(mode: String): Builder {
            mCameraSource.mFlashMode = mode
            return this
        }

        fun setFacing(facing: Int): Builder {
            if (facing != CAMERA_FACING_BACK && facing != CAMERA_FACING_FRONT) {
                throw IllegalArgumentException("Invalid camera: $facing")
            }
            mCameraSource.mFacing = facing
            return this
        }

        /**
         * Sets the desired width and height of the camera frames in pixels.  If the exact desired
         * values are not available options, the best matching available options are selected.
         * Also, we try to select a preview size which corresponds to the aspect ratio of an
         * associated full picture size, if applicable.  Default: 1024x768.
         */
        fun setRequestedPreviewSize(width: Int, height: Int): Builder {
            mCameraSource.mRequestedPreviewWidth = width
            mCameraSource.mRequestedPreviewHeight = height
            return this
        }

        /**
         * Creates an instance of the camera source.
         */
        fun build(): CameraSource {
            mCameraSource.mFrameProcessor = mCameraSource.FrameProcessingRunnable()
            return mCameraSource
        }
    }
}