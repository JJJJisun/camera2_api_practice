package com.jjjjisun.camera2_api

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.Sensor
import android.hardware.SensorManager
import android.hardware.camera2.*
import android.media.ExifInterface
import android.media.ImageReader
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.util.Log
import android.util.SparseIntArray
import android.view.SurfaceHolder
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import com.jjjjisun.camera2_api.databinding.ActivityMainBinding
import kotlinx.android.synthetic.main.activity_main.*
import splitties.toast.toast
import kotlin.jvm.Throws

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private lateinit var mSurfaceViewHolder: SurfaceHolder
    private lateinit var mImageReader: ImageReader
    private lateinit var mCameraDevice: CameraDevice
    private lateinit var mPreviewBuilder: CaptureRequest.Builder
    private lateinit var mSession: CameraCaptureSession

    private var mHandler: Handler? = null

    private lateinit var mAccelerometer: Sensor
    private lateinit var mMagnetometer: Sensor
    private lateinit var mSensorManager: SensorManager

    private val deviceOrientation: DeviceOrientation by lazy { DeviceOrientation() }
    private var mHeight: Int = 0
    private var mWidth: Int = 0

    var mCameraId = CAMERA_BACK

    companion object {
        const val CAMERA_BACK = "0"
        const val CAMERA_FRONT = "1"

        private val ORIENTATIONS = SparseIntArray()

        init {
            ORIENTATIONS.append(ExifInterface.ORIENTATION_NORMAL, 0)
            ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_90, 90)
            ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_180, 180)
            ORIENTATIONS.append(ExifInterface.ORIENTATION_ROTATE_270, 270)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ????????? ?????????
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        // ?????? ?????? ??????
        window.setFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        setContentView(R.layout.activity_main)
        initSensor()
        initView()

    }

    private fun initSensor() {
        mSensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    }

    private fun initView() {
        with(DisplayMetrics()) {
            windowManager.defaultDisplay.getMetrics(this)
            mHeight = heightPixels
            mWidth = widthPixels
        }

        mSurfaceViewHolder = surfaceView.holder
        mSurfaceViewHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(p0: SurfaceHolder) {
                initCameraAndPreview()
            }

            override fun surfaceChanged(
                holder: SurfaceHolder, format: Int,
                width: Int, height: Int
            ) {

            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                mCameraDevice.close()
            }

        })

        btn_convert.setOnClickListener { switchCamera() }

    }

    private fun switchCamera() {
        when (mCameraId) {
            CAMERA_BACK -> {
                mCameraId = CAMERA_FRONT
                mCameraDevice.close()
                openCamera()
            }
            else -> {
                mCameraId = CAMERA_BACK
                mCameraDevice.close()
                openCamera()
            }
        }
    }

    private fun initCameraAndPreview() {
        val handlerThread = HandlerThread("CAMERA2")
        handlerThread.start()
        mHandler = Handler(handlerThread.looper)

        openCamera()
    }

    private fun openCamera() {
        try {
            val mCameManager = this.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val characteristic = mCameManager.getCameraCharacteristics(mCameraId)
            val map = characteristic.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)

            val largestPreviewSize = map!!.getOutputSizes(ImageFormat.JPEG)[0]
            setAspectRatioTextureView(largestPreviewSize.height, largestPreviewSize.width)

            mImageReader = ImageReader.newInstance(
                largestPreviewSize.width,
                largestPreviewSize.height,
                ImageFormat.JPEG,
                7
            )
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED
            ) return

            mCameManager.openCamera(mCameraId, deviceStateCallback, mHandler)
        } catch (e: CameraAccessException) {
            toast("???????????? ?????? ???????????????.")
        }
    }

    private val deviceStateCallback = object : CameraDevice.StateCallback() {
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        override fun onOpened(camera: CameraDevice) {
            mCameraDevice = camera
            try {
                takePreview()
            } catch (e: CameraAccessException) {
                e.printStackTrace()
            }
        }

        @Throws(CameraAccessException::class)
        private fun takePreview() {
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            mPreviewBuilder.addTarget(mSurfaceViewHolder.surface)
            mCameraDevice.createCaptureSession(
                listOf(mSurfaceViewHolder.surface, mImageReader.surface),
                mSessionPreviewStateCallback,
                mHandler
            )
        }

        private val mSessionPreviewStateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                mSession = session
                try {
                    // Key-Value ????????? ??????
                    // ?????????????????? ?????? ??????
                    mPreviewBuilder.set(
                        CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE
                    )
                    //????????? ?????? ???????????? ???????????? ??????
                    mPreviewBuilder.set(
                        CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
                    )
                    mSession.setRepeatingRequest(mPreviewBuilder.build(), null, mHandler)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@MainActivity, "????????? ?????? ??????", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            mCameraDevice.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            toast("???????????? ?????? ???????????????.")
        }

    }

    override fun onResume() {
        super.onResume()

        mSensorManager.registerListener(
            deviceOrientation.eventListener, mAccelerometer, SensorManager.SENSOR_DELAY_UI
        )
        mSensorManager.registerListener(
            deviceOrientation.eventListener, mMagnetometer, SensorManager.SENSOR_DELAY_UI
        )
    }

    override fun onPause() {
        super.onPause()
        mSensorManager.unregisterListener(deviceOrientation.eventListener)
    }

    private fun setAspectRatioTextureView(ResolutionWidth: Int, ResolutionHeight: Int) {
        if (ResolutionWidth > ResolutionHeight) {
            val newWidth = mWidth
            val newHeight = mHeight * ResolutionWidth / ResolutionHeight
            updateTextureViewSize(newWidth, newHeight)
        } else {
            val newWidth = mWidth
            val newHeight = mWidth * ResolutionHeight / ResolutionWidth
            updateTextureViewSize(newWidth, newHeight)
        }
    }

    private fun updateTextureViewSize(viewWidth: Int, viewHeight: Int) {

        Log.d("tag", "TextureView Width : $viewWidth TextureView Height : $viewHeight")
        surfaceView.layoutParams = FrameLayout.LayoutParams(viewWidth, viewHeight)

    }

}