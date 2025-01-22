package dji.sampleV5.aircraft.pages

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import dji.sampleV5.aircraft.R
import dji.sampleV5.aircraft.models.LiveStreamVM
import dji.sampleV5.aircraft.util.ToastUtils
import dji.sampleV5.aircraft.util.ToastUtils.showToast
import dji.sdk.keyvalue.key.DJIKey
import dji.sdk.keyvalue.key.DJIKeyInfo
import dji.sdk.keyvalue.key.FlightControllerKey
import dji.sdk.keyvalue.key.GimbalKey
import dji.sdk.keyvalue.key.KeyTools
import dji.sdk.keyvalue.key.co_u.KeyAircraftLocation3D
import dji.sdk.keyvalue.key.co_v.KeyGimbalMode
import dji.sdk.keyvalue.value.common.Attitude
import dji.sdk.keyvalue.value.common.ComponentIndexType
import dji.sdk.keyvalue.value.common.EmptyMsg
import dji.sdk.keyvalue.value.common.LocationCoordinate3D
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotation
import dji.sdk.keyvalue.value.gimbal.GimbalAngleRotationMode
import dji.sdk.keyvalue.value.gimbal.GimbalMode
import dji.v5.common.callback.CommonCallbacks
import dji.v5.common.error.IDJIError
import dji.v5.manager.KeyManager
import dji.v5.manager.datacenter.MediaDataCenter
import dji.v5.manager.datacenter.livestream.LiveStreamStatus
import dji.v5.manager.datacenter.livestream.LiveVideoBitrateMode
import dji.v5.manager.datacenter.livestream.StreamQuality
import dji.v5.manager.datacenter.livestream.VideoResolution
import dji.v5.manager.interfaces.ICameraStreamManager
import dji.v5.utils.common.StringUtils
import java.io.DataOutputStream
import java.io.IOException
import java.lang.reflect.Array.get
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket


class ServerFragment : DJIFragment() {

    private val cameraStreamManager = MediaDataCenter.getInstance().cameraStreamManager

    private val liveStreamVM: LiveStreamVM by viewModels()

    private val emptyInputMessage = "input is empty"

    private lateinit var cameraIndex: ComponentIndexType
    private lateinit var rgProtocol: RadioGroup
    private lateinit var btnStart: Button
    private lateinit var btnStop: Button
    private lateinit var rgCamera: RadioGroup
    private lateinit var rgQuality: RadioGroup
    private lateinit var rgBitRate: RadioGroup
    private lateinit var sbBitRate: SeekBar
    private lateinit var tvBitRate: TextView
    private lateinit var tvLiveInfo: TextView
    private lateinit var tvLiveError: TextView
    private lateinit var svCameraStream: SurfaceView

    private lateinit var Altitude: TextView
    private lateinit var Longitude: TextView
    private lateinit var Latitude: TextView
    private lateinit var Pitch: TextView
    private lateinit var Roll: TextView
    private lateinit var Yaw: TextView
    private lateinit var Btn_Get_Location: Button
    private lateinit var Btn_Reset_Gimbal: Button
    private lateinit var Btn_Set_Pitchup: Button
    private lateinit var Btn_Set_Pitchdow: Button
    private lateinit var edit_text_IP : EditText
    private var serverSocket: ServerSocket? = null
    private var locationCoordinate3D: LocationCoordinate3D? = null
    private var attitude: Attitude? = null
    private var mHandler = Handler()


    private  var docao : Double = 0.0
    private var kinhdo : Double = 0.0
    private var vido : Double = 0.0

    private  var gocpit : Double = 0.0
    private var gocroll : Double = 0.0
    private var gocyaw : Double = 0.0

    var ip_address = "192.168.0.201"


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_server2, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rgProtocol = view.findViewById(R.id.rg_protocol1)
        btnStart = view.findViewById(R.id.btn_start1)
        btnStop = view.findViewById(R.id.btn_stop1)
        rgCamera = view.findViewById(R.id.rg_camera1)
        rgQuality = view.findViewById(R.id.rg_quality1)
        rgBitRate = view.findViewById(R.id.rg_bit_rate1)
        sbBitRate = view.findViewById(R.id.sb_bit_rate1)
        tvBitRate = view.findViewById(R.id.tv_bit_rate1)
        tvLiveInfo = view.findViewById(R.id.tv_live_info1)
        tvLiveError = view.findViewById(R.id.tv_live_error1)
        svCameraStream = view.findViewById(R.id.sv_camera_stream1)

        initRGCamera()
        initRGQuality()
        initRGBitRate()
        initLiveButton()
        initCameraStream()
        initLiveData()

        Altitude = view.findViewById(R.id.altitude)
        Longitude = view.findViewById(R.id.location_longtitude)
        Latitude = view.findViewById(R.id.location_lattitude)
        Pitch = view.findViewById(R.id.pitch)
        Roll = view.findViewById(R.id.roll)
        Yaw = view.findViewById(R.id.yaw)
        Btn_Get_Location = view.findViewById(R.id.btn_get)
        Btn_Reset_Gimbal = view.findViewById(R.id.btn_reset_gimbal)
        Btn_Set_Pitchup = view.findViewById(R.id.btn_setup)
        Btn_Set_Pitchdow = view.findViewById(R.id.btn_setdow)
        edit_text_IP = view.findViewById(R.id.edittextIP)




        Btn_Set_Pitchup.setOnClickListener {
            rotateGimbalup()
        }

        Btn_Set_Pitchdow.setOnClickListener {
            rotateGimbaldow()
        }
        Btn_Reset_Gimbal.setOnClickListener {
            resetGimbal()

        }

        Btn_Get_Location.setOnClickListener {
            ip_address = edit_text_IP.text.toString()
            ToastUtils.showToast(ip_address)
        }

        mHandler.postDelayed(mRunnable, 1000)


    }
        // Gia tri luu vi vi tri
//        Btn_Get_Location.setOnClickListener {
//            showLocationValues() ;showGimbal()

    private val mRunnable = object : Runnable {
        override fun run() {

            showLocationValues()
            showGimbal()
            var latitudeValue = locationCoordinate3D?.latitude
            var longitudeValue = locationCoordinate3D?.longitude
            var altitudeValue = locationCoordinate3D?.altitude

            Altitude.text = altitudeValue.toString()
            Longitude.text = longitudeValue.toString()
            Latitude.text = latitudeValue.toString()

            var pitchValue = attitude?.pitch
            var rollValue = attitude?.roll
            var yawValue = attitude?.yaw

            Pitch.text = pitchValue.toString()
            Roll.text = rollValue.toString()
            Yaw.text = yawValue.toString()



            object : Thread() {
                var socket: Socket? = null
                var host = ip_address// Replace with the actual IP address of the server
                var port = 8999
                override fun run() {
                    try {
                        // Establish connection
                        Log.i("Connection", "Connecting to $host:$port")
                        val serverAddr = InetAddress.getByName(host)
                        socket = Socket(serverAddr, port)
                        Log.i("Connection", "Connected!")
                        // Get the output stream and send messages through this stream
                        val out = DataOutputStream(socket!!.getOutputStream())
                        //Send image message

                        if (locationCoordinate3D != null && attitude != null) {
                            var latitudeValue = locationCoordinate3D?.latitude
                            var longitudeValue = locationCoordinate3D?.longitude
                            var altitudeValue = locationCoordinate3D?.altitude

                            var pitchValue = attitude?.pitch
                            var rollValue = attitude?.roll
                            var yawValue = attitude?.yaw


                            if (altitudeValue != null) {
                                docao = altitudeValue
                            }
                            if (longitudeValue != null) {
                                kinhdo = longitudeValue
                            }
                            if (latitudeValue != null) {
                                vido = latitudeValue
                            }

                            if (pitchValue != null) {
                                gocpit = pitchValue
                            }
                            if (rollValue != null) {
                                gocroll = rollValue
                            }
                            if (yawValue != null) {
                                gocyaw = yawValue
                            }

                            sendTextMsg(out, kinhdo.toString() +" "+ vido.toString() +" "+ docao.toString()+" "+ gocpit.toString() +" "+ gocroll.toString() +" "+ gocyaw.toString())
                        }
                        else
                        {
                            sendTextMsg(out, "error!!" )
                        }


//                    // Receive response from the server
//                    val reader = BufferedReader(
//                        InputStreamReader(
//                            socket!!.getInputStream()
//                        )
//                    )
//                    val receivedString = reader.readLine()
//                    val numericString = receivedString.replace("[^0-9]".toRegex(), "")
//                    Log.d("Received", numericString)
                        // Close the connection
                        out.close()
                        socket!!.close()
                        Log.i("Connection", "Disconnected!")

                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }.start()
            mHandler.postDelayed(this, 1000)

        }


    }




    @Throws(IOException::class)
    fun sendTextMsg(out: DataOutputStream, msg: String) {
        val bytes = msg.toByteArray()
        val len = bytes.size.toLong()
        // Send the length first, then send the content
        out.writeLong(len)
        out.write(bytes)
    }



    private fun rotateGimbalup() {
        val pitchAngle = attitude?.pitch?.plus(10)
        val yawAngle = 0F
        val gimbalAngleRotation = GimbalAngleRotation()

        gimbalAngleRotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE)
        gimbalAngleRotation.setPitchIgnored (true)
        gimbalAngleRotation.setYawIgnored(true)
        gimbalAngleRotation.setRollIgnored(true)
        if (!java.lang.Double.isNaN(pitchAngle!!)) {
            gimbalAngleRotation.setPitch(pitchAngle!!.toDouble())
            gimbalAngleRotation.setPitchIgnored(false)
        }

        if (java.lang.Float.isNaN(yawAngle)) {
            gimbalAngleRotation.setYaw(yawAngle.toDouble())
            gimbalAngleRotation.setYawIgnored(false)
        }
        gimbalAngleRotation.setDuration(4.0)
        KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle), gimbalAngleRotation, object : CommonCallbacks.CompletionCallbackWithParam <EmptyMsg> {
            override fun onSuccess(t: EmptyMsg?) {
                ToastUtils.showToast("enableSetGimbal success.")
            }


            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("setGimbalAngle, $error")
            }
        })

    }
    private fun rotateGimbaldow() {
        val pitchAngle = attitude?.pitch?.minus(10)
        val yawAngle = 0F
        val gimbalAngleRotation = GimbalAngleRotation()

        gimbalAngleRotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE)
        gimbalAngleRotation.setPitchIgnored (true)
        gimbalAngleRotation.setYawIgnored(true)
        gimbalAngleRotation.setRollIgnored(true)
        if (!java.lang.Double.isNaN(pitchAngle!!)) {
            gimbalAngleRotation.setPitch(pitchAngle!!.toDouble())
            gimbalAngleRotation.setPitchIgnored(false)
        }

        if (java.lang.Float.isNaN(yawAngle)) {
            gimbalAngleRotation.setYaw(yawAngle.toDouble())
            gimbalAngleRotation.setYawIgnored(false)
        }
        gimbalAngleRotation.setDuration(10.0)
        KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle), gimbalAngleRotation, object : CommonCallbacks.CompletionCallbackWithParam <EmptyMsg> {
            override fun onSuccess(t: EmptyMsg?) {
                ToastUtils.showToast("enableSetGimbal success.")
            }


            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("setGimbalAngle, $error")
            }
        })

    }

    private fun resetGimbal() {
        val pitchAngle = 0F
        val yawAngle = 0F
        val gimbalAngleRotation = GimbalAngleRotation()

        gimbalAngleRotation.setMode(GimbalAngleRotationMode.ABSOLUTE_ANGLE)
        gimbalAngleRotation.setPitchIgnored (true)
        gimbalAngleRotation.setYawIgnored(true)
        gimbalAngleRotation.setRollIgnored(true)
        if (!java.lang.Float.isNaN(pitchAngle!!)) {
            gimbalAngleRotation.setPitch(pitchAngle!!.toDouble())
            gimbalAngleRotation.setPitchIgnored(false)
        }

        if (java.lang.Float.isNaN(yawAngle)) {
            gimbalAngleRotation.setYaw(yawAngle.toDouble())
            gimbalAngleRotation.setYawIgnored(false)
        }
        gimbalAngleRotation.setDuration(10.0)
        KeyManager.getInstance().performAction(KeyTools.createKey(GimbalKey.KeyRotateByAngle), gimbalAngleRotation, object : CommonCallbacks.CompletionCallbackWithParam <EmptyMsg> {
            override fun onSuccess(t: EmptyMsg?) {
                ToastUtils.showToast("enableSetGimbal success.")
            }


            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("setGimbalAngle, $error")
            }
        })

    }

    private fun showLocationValues() {
        KeyManager.getInstance().getValue(KeyTools.createKey(FlightControllerKey.KeyAircraftLocation3D), object : CommonCallbacks.CompletionCallbackWithParam<LocationCoordinate3D> {
            override fun onSuccess(t: LocationCoordinate3D?) {
                locationCoordinate3D = t
                val mode : DJIKeyInfo<LocationCoordinate3D>? = KeyAircraftLocation3D
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("Failed to get GPS data: $error")
            }
        })
    }

    private fun showGimbal(){
        KeyManager.getInstance().getValue(KeyTools.createKey(GimbalKey.KeyGimbalAttitude), object: CommonCallbacks.CompletionCallbackWithParam<Attitude> {
            override fun onSuccess(t:Attitude?) {
                attitude = t
            }

            override fun onFailure(error: IDJIError) {
                ToastUtils.showToast("Failed to get Gimbal data: $error")
            }
        })

    }

    override fun onDestroyView() {
        super.onDestroyView()
        serverSocket?.close()
        mHandler.removeCallbacks(mRunnable)
        stopLive()
    }


    @SuppressLint("SetTextI18n")
    private fun initLiveData() {
        liveStreamVM.liveStreamStatus.observe(viewLifecycleOwner) { status ->
            var liveStreamStatus = status
            if (liveStreamStatus == null) {
                liveStreamStatus = LiveStreamStatus(0, 0, 0, 0, 0, false, VideoResolution(0, 0))
            }

            tvLiveInfo.text = liveStreamStatus.toString()
            rgProtocol.isEnabled = !liveStreamStatus.isStreaming
            for (i in 0 until rgProtocol.childCount) {
                rgProtocol.getChildAt(i).isEnabled = rgProtocol.isEnabled
            }
            btnStart.isEnabled = !liveStreamVM.isStreaming()
            btnStop.isEnabled = liveStreamVM.isStreaming()
        }

        liveStreamVM.liveStreamError.observe(viewLifecycleOwner) { error ->
            if (error == null) {
                tvLiveError.text = ""
                tvLiveError.visibility = View.GONE
            } else {
                tvLiveError.text = "error : $error"
                tvLiveError.visibility = View.VISIBLE
            }
        }

        liveStreamVM.availableCameraList.observe(viewLifecycleOwner) { cameraIndexList ->
            var firstAvailableView: View? = null
            var isNeedChangeCamera = false
            for (i in 0 until rgCamera.childCount) {
                val view = rgCamera.getChildAt(i)
                val index = ComponentIndexType.find((view.tag as String).toInt())
                if (cameraIndexList.contains(index)) {
                    view.visibility = View.VISIBLE
                    if (firstAvailableView == null) {
                        firstAvailableView = view
                    }
                } else {
                    view.visibility = View.GONE
                    if (rgCamera.checkedRadioButtonId == view.id) {
                        isNeedChangeCamera = true
                    }
                }
            }
            if (isNeedChangeCamera && firstAvailableView != null) {
                rgCamera.check(firstAvailableView.id)
            }
            if (cameraIndexList.isEmpty()) {
                stopLive()
            }
        }
    }

    private fun initRGCamera() {
        rgCamera.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            cameraIndex = ComponentIndexType.find((view.tag as String).toInt())
            val surface = svCameraStream.holder.surface
            if (surface != null && svCameraStream.width != 0) {
                cameraStreamManager.putCameraStreamSurface(
                    cameraIndex,
                    surface,
                    svCameraStream.width,
                    svCameraStream.height,
                    ICameraStreamManager.ScaleType.CENTER_INSIDE
                )
            }
            liveStreamVM.setCameraIndex(cameraIndex)
        }
        rgCamera.check(R.id.rb_camera_left1)
    }

    private fun initRGQuality() {
        rgQuality.setOnCheckedChangeListener { group: RadioGroup, checkedId: Int ->
            val view = group.findViewById<View>(checkedId)
            liveStreamVM.setLiveStreamQuality(StreamQuality.find((view.tag as String).toInt()))
        }
        rgQuality.check(R.id.rb_quality_hd1)
    }

    @SuppressLint("SetTextI18n")
    private fun initRGBitRate() {
        rgBitRate.setOnCheckedChangeListener { _: RadioGroup?, checkedId: Int ->
            if (checkedId == R.id.rb_bit_rate_auto) {
                sbBitRate.visibility = View.GONE
                tvBitRate.visibility = View.GONE
                liveStreamVM.setLiveVideoBitRateMode(LiveVideoBitrateMode.AUTO)
            } else if (checkedId == R.id.rb_bit_rate_manual1) {
                sbBitRate.visibility = View.VISIBLE
                tvBitRate.visibility = View.VISIBLE
                liveStreamVM.setLiveVideoBitRateMode(LiveVideoBitrateMode.MANUAL)
                sbBitRate.progress = 20
            }
        }
        sbBitRate.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                tvBitRate.text = (bitRate / 8 / 1024).toString() + " vbbs"
                if (!fromUser) {
                    liveStreamVM.setLiveVideoBitRate(bitRate)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                liveStreamVM.setLiveVideoBitRate(bitRate)
            }

            private val bitRate: Int
                get() = (8 * 1024 * 2048 * (0.1 + 0.9 * sbBitRate.progress / sbBitRate.max)).toInt()
        })
        rgBitRate.check(R.id.rb_bit_rate_auto1)
    }

    private fun initLiveButton() {
        btnStart.setOnClickListener { _ ->
            val protocolCheckId = rgProtocol.checkedRadioButtonId
            if (protocolCheckId == R.id.rb_rtmp1) {
                showSetLiveStreamRtmpConfigDialog()
            } else if (protocolCheckId == R.id.rb_rtsp1) {
                showSetLiveStreamRtspConfigDialog()
            } else if (protocolCheckId == R.id.rb_gb281811) {
                showSetLiveStreamGb28181ConfigDialog()
            } else if (protocolCheckId == R.id.rb_agora1) {
                showSetLiveStreamAgoraConfigDialog()
            }
        }
        btnStop.setOnClickListener {
            stopLive()
        }
    }

    private fun initCameraStream() {
        svCameraStream.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {}
            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                if (cameraIndex != ComponentIndexType.UNKNOWN) {
                    cameraStreamManager.putCameraStreamSurface(
                        cameraIndex,
                        holder.surface,
                        width,
                        height,
                        ICameraStreamManager.ScaleType.CENTER_INSIDE
                    )
                }
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                cameraStreamManager.removeCameraStreamSurface(holder.surface)
            }
        })
    }

    private fun startLive() {
        if (!liveStreamVM.isStreaming()) {
            liveStreamVM.startStream(object : CommonCallbacks.CompletionCallback {
                override fun onSuccess() {
                    ToastUtils.showShortToast(StringUtils.getResStr(R.string.msg_start_live_stream_success))
                }

                override fun onFailure(error: IDJIError) {
                    ToastUtils.showLongToast(
                        StringUtils.getResStr(R.string.msg_start_live_stream_failed, error.description())
                    )
                }
            });
        }
    }

    private fun stopLive() {
        liveStreamVM.stopStream(null)
    }

    private fun showSetLiveStreamRtmpConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val rtmpConfigView = factory.inflate(R.layout.dialog_livestream_rtmp_config_view, null)
        val etRtmpUrl = rtmpConfigView.findViewById<EditText>(R.id.et_livestream_rtmp_config)
        etRtmpUrl.setText(
            liveStreamVM.getRtmpUrl().toCharArray(),
            0,
            liveStreamVM.getRtmpUrl().length
        )
        val configDialog = requireContext().let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtmp_config)
                .setCancelable(false)
                .setView(rtmpConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputValue = etRtmpUrl.text.toString()
                        if (TextUtils.isEmpty(inputValue)) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            liveStreamVM.setRTMPConfig(inputValue)
                            startLive()
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamRtspConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val rtspConfigView = factory.inflate(R.layout.dialog_livestream_rtsp_config_view, null)
        val etRtspUsername = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_username)
        val etRtspPassword = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_password)
        val etRtspPort = rtspConfigView.findViewById<EditText>(R.id.et_livestream_rtsp_port)
        val rtspConfig = liveStreamVM.getRtspSettings()
        if (!TextUtils.isEmpty(rtspConfig) && rtspConfig.isNotEmpty()) {
            val configs = rtspConfig.trim().split("^_^")
            etRtspUsername.setText(
                configs[0].toCharArray(),
                0,
                configs[0].length
            )
            etRtspPassword.setText(
                configs[1].toCharArray(),
                0,
                configs[1].length
            )
            etRtspPort.setText(
                configs[2].toCharArray(),
                0,
                configs[2].length
            )
        }

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_rtsp_config)
                .setCancelable(false)
                .setView(rtspConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val inputUserName = etRtspUsername.text.toString()
                        val inputPassword = etRtspPassword.text.toString()
                        val inputPort = etRtspPort.text.toString()
                        if (TextUtils.isEmpty(inputUserName) || TextUtils.isEmpty(inputPassword) || TextUtils.isEmpty(
                                inputPort
                            )
                        ) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            try {
                                liveStreamVM.setRTSPConfig(
                                    inputUserName,
                                    inputPassword,
                                    inputPort.toInt()
                                )
                                startLive()
                            } catch (e: NumberFormatException) {
                                ToastUtils.showToast("RTSP port must be int value")
                            }
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamGb28181ConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val gbConfigView = factory.inflate(R.layout.dialog_livestream_gb28181_config_view, null)
        val etGbServerIp = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_ip)
        val etGbServerPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_port)
        val etGbServerId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_server_id)
        val etGbAgentId = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_agent_id)
        val etGbChannel = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_channel)
        val etGbLocalPort = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_local_port)
        val etGbPassword = gbConfigView.findViewById<EditText>(R.id.et_livestream_gb28181_password)

        val gbConfig = liveStreamVM.getGb28181Settings()
        if (!TextUtils.isEmpty(gbConfig) && gbConfig.isNotEmpty()) {
            val configs = gbConfig.trim().split("^_^")
            etGbServerIp.setText(
                configs[0].toCharArray(),
                0,
                configs[0].length
            )
            etGbServerPort.setText(
                configs[1].toCharArray(),
                0,
                configs[1].length
            )
            etGbServerId.setText(
                configs[2].toCharArray(),
                0,
                configs[2].length
            )
            etGbAgentId.setText(
                configs[3].toCharArray(),
                0,
                configs[3].length
            )
            etGbChannel.setText(
                configs[4].toCharArray(),
                0,
                configs[4].length
            )
            etGbLocalPort.setText(
                configs[5].toCharArray(),
                0,
                configs[5].length
            )
            etGbPassword.setText(
                configs[6].toCharArray(),
                0,
                configs[6].length
            )
        }

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_gb28181_config)
                .setCancelable(false)
                .setView(gbConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val serverIp = etGbServerIp.text.toString()
                        val serverPort = etGbServerPort.text.toString()
                        val serverId = etGbServerId.text.toString()
                        val agentId = etGbAgentId.text.toString()
                        val channel = etGbChannel.text.toString()
                        val localPort = etGbLocalPort.text.toString()
                        val password = etGbPassword.text.toString()
                        if (TextUtils.isEmpty(serverIp) || TextUtils.isEmpty(serverPort) || TextUtils.isEmpty(
                                serverId
                            ) || TextUtils.isEmpty(agentId) || TextUtils.isEmpty(channel) || TextUtils.isEmpty(
                                localPort
                            ) || TextUtils.isEmpty(password)
                        ) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            try {
                                liveStreamVM.setGB28181(
                                    serverIp,
                                    serverPort.toInt(),
                                    serverId,
                                    agentId,
                                    channel,
                                    localPort.toInt(),
                                    password
                                )
                                startLive()
                            } catch (e: NumberFormatException) {
                                ToastUtils.showToast("RTSP port must be int value")
                            }
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }

    private fun showSetLiveStreamAgoraConfigDialog() {
        val factory = LayoutInflater.from(requireContext())
        val agoraConfigView = factory.inflate(R.layout.dialog_livestream_agora_config_view, null)

        val etAgoraChannelId = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_channel_id)
        val etAgoraToken = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_token)
        val etAgoraUid = agoraConfigView.findViewById<EditText>(R.id.et_livestream_agora_uid)

        val agoraConfig = liveStreamVM.getAgoraSettings()
        if (!TextUtils.isEmpty(agoraConfig) && agoraConfig.length > 0) {
            val configs = agoraConfig.trim().split("^_^")
            etAgoraChannelId.setText(configs[0].toCharArray(), 0, configs[0].length)
            etAgoraToken.setText(configs[1].toCharArray(), 0, configs[1].length)
            etAgoraUid.setText(configs[2].toCharArray(), 0, configs[2].length)
        }

        val configDialog = requireContext().let {
            AlertDialog.Builder(it, R.style.Base_ThemeOverlay_AppCompat_Dialog_Alert)
                .setIcon(android.R.drawable.ic_menu_camera)
                .setTitle(R.string.ad_set_live_stream_agora_config)
                .setCancelable(false)
                .setView(agoraConfigView)
                .setPositiveButton(R.string.ad_confirm) { configDialog, _ ->
                    kotlin.run {
                        val channelId = etAgoraChannelId.text.toString()
                        val token = etAgoraToken.text.toString()
                        val uid = etAgoraUid.text.toString()
                        if (TextUtils.isEmpty(channelId) || TextUtils.isEmpty(token) || TextUtils.isEmpty(
                                uid
                            )
                        ) {
                            ToastUtils.showToast(emptyInputMessage)
                        } else {
                            liveStreamVM.setAgoraConfig(channelId, token, uid)
                            startLive()
                        }
                        configDialog.dismiss()
                    }
                }
                .setNegativeButton(R.string.ad_cancel) { configDialog, _ ->
                    kotlin.run {
                        configDialog.dismiss()
                    }
                }
                .create()
        }
        configDialog.show()
    }




}



