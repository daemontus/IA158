package com.daemontus.vidar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Button
import android.widget.SeekBar
import android.widget.Switch
import org.opencv.android.*
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
import rx.Observable
import rx.Subscription
import rx.schedulers.Schedulers
import rx.subjects.PublishSubject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : Activity(), CameraBridgeViewBase.CvCameraViewListener2 {

    private val TAG = "VIDAR"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main)
        createOpenCV()
    }

    override fun onPause() {
        pauseBroadcast()
        pauseOpenCV()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        resumeOpenCV()
        resumeBroadcast()
    }

    // =========== Broadcast related part ==========

    // specific broadcast addresses for each network calculated from the IP and subnet mask
    @Suppress("unused")
    private val robot = "10.0.1.255"
    @Suppress("unused")
    private val eduroam = "147.251.45.255"
    private val broadcastAddress = robot
    private val port = 9999
/*
    enum class Action {
        LEFT, RIGHT, SHOOT, NONE;

        fun toByte(): Byte {
            return when (this) {
                Action.LEFT -> 1
                Action.RIGHT -> 2
                Action.SHOOT -> 3
                Action.NONE -> 4
            }.toByte()
        }

    }
*/
    // subject responsible for transferring actions to the background thread observer
    // which will send them as a broadcast
    private val actionSubject = PublishSubject.create<Byte>()

    // holds current broadcast subscription - unsubscribe to stop sending messages
    private var broadcastTask: Subscription? = null

    private fun resumeBroadcast() {
        // This cluster-fuck should ensure the socket is closed as soon as the subscription
        // is canceled.
        broadcastTask = Observable.using<List<Byte>, DatagramSocket>(
                // resource factory
                { DatagramSocket(port) },
                // observable factory
                { socket ->
                    // create new observable that will send a broadcast on each action
                    // while throttling the actions and dropping them if they are still too fast.
                    actionSubject
                            .buffer(2)
                            .throttleLast(100, TimeUnit.MILLISECONDS)
                            .onBackpressureDrop()
                            .observeOn(Schedulers.io())
                            .doOnNext { coords ->
                                println("send action: $coords")
                                val group = InetAddress.getByName(broadcastAddress)
                                val buffer = coords.toByteArray()
                                val packet = DatagramPacket(buffer, buffer.size, group, 9999)
                                socket.send(packet)
                            }
                },
                //cleanup action
                DatagramSocket::close, true)
                .subscribeOn(Schedulers.io())   //just to be sure
                .subscribe()
    }

    private fun pauseBroadcast() {
        broadcastTask?.unsubscribe()
    }


    // =========== OpenCV related part ==========

    // holds reference to the rendering surface
    lateinit var cameraView: JavaCameraView
    lateinit var calibrateView: Button
    lateinit var HtoleranceView: SeekBar
    lateinit var StoleranceView: SeekBar
    lateinit var VtoleranceView: SeekBar
    lateinit var showImageView: Switch

    private var color: Scalar? = Scalar(13.5, 175.0, 250.0) // green Scalar(37.0, 120.0, 239.0)

    // used when initializing OpenCV
    private val loadCallback = object : BaseLoaderCallback(this) {
        override fun onManagerConnected(status: Int) {
            when (status) {
                LoaderCallbackInterface.SUCCESS -> cameraView.enableView()
                else -> super.onManagerConnected(status)
            }
        }
    }

    // should be called in onCreate
    private fun createOpenCV() {
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        showImageView = findViewById(R.id.show_image) as Switch
        calibrateView = findViewById(R.id.calibrate) as Button
        calibrateView.setOnClickListener {
            color = null
        }
        HtoleranceView = findViewById(R.id.H_tolerance) as SeekBar
        HtoleranceView.max = 30
        HtoleranceView.progress = 5
        StoleranceView = findViewById(R.id.S_tolerance) as SeekBar
        StoleranceView.max = 100
        StoleranceView.progress = 40
        VtoleranceView = findViewById(R.id.V_tolerance) as SeekBar
        VtoleranceView.max = 100
        VtoleranceView.progress = 40
        cameraView = findViewById(R.id.camera) as JavaCameraView
        cameraView.setCvCameraViewListener(this)
    }

    // should be called in onResume
    private fun resumeOpenCV() {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization")
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, loadCallback)
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!")
            loadCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS)
        }
    }

    // should be called in onPause
    private fun pauseOpenCV() {
        cameraView.disableView()
    }

    // holds the camera image converted to the BGR format
    private var bgr: Mat? = null
    // holds the camera image converted to HSV format
    private var hsv: Mat? = null
    // holds the image that is being analysed for objects
    private var thresholded: Mat? = null

    private var nativeData: ByteArray? = null

    private var output: Mat? = null

    private val one = Scalar(1.0, 1.0, 1.0)

    // init all matrices
    override fun onCameraViewStarted(width: Int, height: Int) {
        hsv = Mat(width, height, CvType.CV_8UC4)
        bgr = Mat(width, height, CvType.CV_8UC4)
        thresholded = Mat(width, height, CvType.CV_8UC4)
        output = Mat(width, height, CvType.CV_8UC4)
        nativeData = ByteArray(width * height)
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        val hsv = hsv!!
        val bgr = bgr!!
        // convert camera image to HSV
        Imgproc.cvtColor(inputFrame.rgba(), bgr, Imgproc.COLOR_RGBA2BGR)
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)

        val thresholded = this.thresholded!!

        // hue - 0-179
        // saturation - 0-255
        // value - 0-255
        // tennis ball (bright) - 37/120/239
        if (color == null) {
            val minDim = Math.min(hsv.width(), hsv.height())
            val cutWidth = (hsv.width() - minDim) / 2
            val cutHeight = (hsv.height() - minDim) / 2
            Log.d(TAG, "Hsv size: ${hsv.width()} ${hsv.height()}")
            Log.d(TAG, "Ranges: ${cutWidth} ${ cutWidth + minDim} ; $cutHeight ${ cutHeight + minDim }")
            color = Core.mean(Mat(hsv, Range(cutHeight, cutHeight + minDim), Range(cutWidth, cutWidth + minDim)))
            //color = Scalar(hsv[hsv.width()/2, hsv.height()/2])
            Log.d(TAG, "New color: ${color}")
        }
        val color = this.color!!
        Log.d(TAG, "Color $color")
        val min = Scalar(
                color.`val`[0] - HtoleranceView.progress,
                color.`val`[1] - StoleranceView.progress,
                color.`val`[2] - VtoleranceView.progress
        )
        val max = Scalar(
                color.`val`[0] + HtoleranceView.progress,
                color.`val`[1] + StoleranceView.progress,
                color.`val`[2] + VtoleranceView.progress
        )
        Log.d(TAG, "Min: $min")
        Log.d(TAG, "Max: $max")
        //Core.inRange(hsv, Scalar(30.0, 104.0, 104.0), Scalar(40.0, 255.0, 255.0), thresholded)
        Core.inRange(hsv, min, max, thresholded)
        //draw the "finder"
        Imgproc.rectangle(hsv, Point(hsv.width()/2 - 5.0, hsv.height()/2 - 5.0), Point(hsv.width()/2 + 5.0, hsv.height()/2 + 5.0), Scalar(250.0, 250.0, 250.0))

        var sumX = 0
        var sumY = 0
        var count = 0

        val height = thresholded.height()
        val width = thresholded.width()

        nativeData?.let { nativeData ->
            thresholded.get(0,0, nativeData)
            for (i in (0 until height)) {
                for (j in (0 until width)) {
                    val isUp = if (if (i == 0) true else {
                        nativeData[(i - 1) * width + j] < 0
                    }) 1 else 0
                    val isDown = if (if (i == height - 1) true else {
                        nativeData[(i + 1) * width + j] < 0
                    }) 1 else 0
                    val isLeft = if (if (j == 0) true else {
                        nativeData[i * width + j - 1] < 0
                    }) 1 else 0
                    val isRight = if (if (j == width - 1) true else {
                        nativeData[i * width + j + 1] < 0
                    }) 1 else 0
                    if (nativeData[i * width + j] < 0 && isUp + isDown + isLeft + isRight > 2) {  // -1 == 255 in java bytes
                        sumY += i
                        sumX += j
                        count += 1
                    }
                }
            }
        }

        if (count > 100) {
            val posX = sumX / count.toDouble()
            val posY = sumY / count.toDouble()

            Log.d(TAG, "Sum: $sumX, $sumY Avr: $posX, $posY")

            // draw a rectangle around the object (the size - 200x200 - is arbitrary)
            Imgproc.rectangle(hsv, Point(posX - 100, posY - 100), Point(posX + 100, posY + 100), Scalar(250.0, 250.0, 250.0))
            Imgproc.rectangle(thresholded, Point(posX - 100, posY - 100), Point(posX + 100, posY + 100), Scalar(250.0, 250.0, 250.0))

            val percentX = (posX / width.toDouble()) * 100
            val percentY = (posY / height.toDouble()) * 100

            actionSubject.onNext((if (percentX in (0..100)) percentX else -1.0).toByte())
            actionSubject.onNext((if (percentY in (0..100)) (100 - percentY) else -1.0).toByte())
        } else {
            actionSubject.onNext(-1)
            actionSubject.onNext(-1)
        }


        // This should smoothen the image a little so that the small fragments don't
        // disturb the results.

        //Imgproc.erode(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))
        //Imgproc.dilate(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))

        //Imgproc.dilate(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))
        //Imgproc.erode(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))

        // Detect "moments" in the image - i.e. object location and size if there is just one object


/*
        val moments = Imgproc.moments(thresholded)

        val dM01 = moments._m01
        val dM10 = moments._m10
        val dArea = moments._m00

        val percentX: Double
        val percentY: Double
        if (dArea >= 1000.0) {
            val posX = dM10 / dArea
            val posY = dM01 / dArea

            // relative X position
            percentX = (posX / thresholded.width()) * 100.0
            percentY = (posY / thresholded.height()) * 100.0

            // draw a rectangle around the object (the size - 200x200 - is arbitrary)
            Imgproc.rectangle(hsv, Point(posX - 100, posY - 100), Point(posX + 100, posY + 100), Scalar(250.0, 250.0, 250.0))
            Imgproc.rectangle(thresholded, Point(posX - 100, posY - 100), Point(posX + 100, posY + 100), Scalar(250.0, 250.0, 250.0))
        } else {
            percentX = -1.0
            percentY = -1.0
        }

        // publish the current relative position as an action
        println("action: $percentX")
        /*val action = when (percentX) {
            in (0..45) -> Action.LEFT// actionSubject.onNext(Action.LEFT)
            in (45..55) -> Action.SHOOT// actionSubject.onNext(Action.SHOOT)
            in (55..100) -> Action.RIGHT// actionSubject.onNext(Action.RIGHT)
            else -> Action.NONE// actionSubject.onNext(Action.NONE)
        }*/

        actionSubject.onNext((if (percentX in (0..100)) percentX else -1.0).toByte())
        actionSubject.onNext((if (percentY in (0..100)) (100 - percentY) else -1.0).toByte())
*/
        return if (showImageView.isChecked) {
            hsv
        } else {
            thresholded
        }
    }

    // clean up
    override fun onCameraViewStopped() {
        hsv?.release()
        bgr?.release()
        thresholded?.release()
    }
}