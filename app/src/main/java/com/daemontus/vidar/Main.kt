package com.daemontus.vidar

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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

    // subject responsible for transferring actions to the background thread observer
    // which will send them as a broadcast
    private val actionSubject = PublishSubject.create<Action>()

    // holds current broadcast subscription - unsubscribe to stop sending messages
    private var broadcastTask: Subscription? = null

    private fun resumeBroadcast() {
        // This cluster-fuck should ensure the socket is closed as soon as the subscription
        // is canceled.
        broadcastTask = Observable.using<Action, DatagramSocket>(
                // resource factory
                { DatagramSocket(port) },
                // observable factory
                { socket ->
                    // create new observable that will send a broadcast on each action
                    // while throttling the actions and dropping them if they are still too fast.
                    actionSubject
                            .throttleLast(20, TimeUnit.MILLISECONDS)
                            .onBackpressureDrop()
                            .observeOn(Schedulers.io())
                            .doOnNext { action ->
                                Log.d(TAG, "Send action: $action")
                                val buffer = ByteArray(1)
                                buffer[0] = action.toByte()
                                val group = InetAddress.getByName(broadcastAddress)
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

    // init all matrices
    override fun onCameraViewStarted(width: Int, height: Int) {
        hsv = Mat(width, height, CvType.CV_8UC4)
        bgr = Mat(width, height, CvType.CV_8UC4)
        thresholded = Mat(width, height, CvType.CV_8UC4)
    }

    override fun onCameraFrame(inputFrame: CameraBridgeViewBase.CvCameraViewFrame): Mat {
        // convert camera image to HSV
        Imgproc.cvtColor(inputFrame.rgba(), bgr, Imgproc.COLOR_RGBA2BGR)
        Imgproc.cvtColor(bgr, hsv, Imgproc.COLOR_BGR2HSV)

        val thresholded = this.thresholded!!

        // hue - 0-179
        // saturation - 0-255
        // value - 0-255
        // tennis ball (bright) - 37/120/239
        Core.inRange(hsv, Scalar(30.0, 104.0, 104.0), Scalar(40.0, 255.0, 255.0), thresholded)

        // This should smoothen the image a little so that the small fragments don't
        // disturb the results.

        Imgproc.erode(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))
        Imgproc.dilate(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))

        Imgproc.dilate(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))
        Imgproc.erode(thresholded, thresholded, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(5.0,5.0)))

        // Detect "moments" in the image - i.e. object location and size if there is just one object

        val moments = Imgproc.moments(thresholded)

        val dM01 = moments._m01
        val dM10 = moments._m10
        val dArea = moments._m00

        val percentX: Double
        if (dArea >= 10000.0) {
            val posX = dM10 / dArea
            val posY = dM01 / dArea

            // relative X position
            percentX = (posX / thresholded.width()) * 100.0

            // draw a rectangle around the object (the size - 200x200 - is arbitrary)
            Imgproc.rectangle(thresholded, Point(posX - 100, posY - 100), Point(posX + 100, posY + 100), Scalar(250.0, 250.0, 250.0))
        } else {
            percentX = -1.0
        }

        // publish the current relative position as an action
        when (percentX) {
            in (0..40) -> actionSubject.onNext(Action.LEFT)
            in (45..55) -> actionSubject.onNext(Action.SHOOT)
            in (60..100) -> actionSubject.onNext(Action.RIGHT)
            else -> actionSubject.onNext(Action.NONE)
        }

        return thresholded
    }

    // clean up
    override fun onCameraViewStopped() {
        hsv?.release()
        bgr?.release()
        thresholded?.release()
    }
}