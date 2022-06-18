package com.example.blosetk

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Bundle
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timer


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

class MainActivity : AppCompatActivity(), LifecycleOwner {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private lateinit var inferredCategoryText: TextView
    private lateinit var inferredScoreText: TextView
    private lateinit var activateCameraBtn: Button
    var labelarray = ArrayList<String>() // 같은 라벨인지 확인하기 위한 ArrayList
    lateinit var currentCate: String
    var samelabeltest: Boolean = false // 같은 라벨인지 확인
    var labelteststatus: Boolean = false // 테스트 진행중인지
    var isSpeak: Boolean = false // 음성 재생 중인지

    var firstlabel: String = ""
    var secondlabel: String = ""
    var thirdlabel: String = ""

    var dialogstr : String = "" // 다이얼로그 문장
    var dialogcheck : Boolean = false // 다이얼로그 실행중인지
    var faildialog : Boolean = false // 인식 실패 다이얼로그 실행중인지

    lateinit var tts: TextToSpeech

    // 타이머 관련
    private var time = 0
    private var timerTask : Timer? =null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        viewFinder = findViewById(R.id.view_finder)
        inferredCategoryText = findViewById(R.id.inferredCategoryText);
        inferredScoreText = findViewById(R.id.inferredScoreText);
        activateCameraBtn = findViewById(R.id.activateCameraBtn);



        // 카메라 동작 (버튼식 -> 자동)
        if (allPermissionsGranted()) {
            viewFinder.post { startCamera() }
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        viewFinder.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            updateTransform()
        }



        currentCate = ""
        val t0: TimerTask = object : TimerTask() {
            override fun run() {
                Log.d("실행중", "t0 실행중")
                Log.d("labelarray size: ", labelarray.size.toString())
                // 3초동안 동일한 라벨이어야 해당 라벨로 인정
                labelarray.add(currentCate)
                Log.d("labelarray", labelarray.toString())
            }
        }

        val t1: TimerTask = object : TimerTask() {
            override fun run() {
                // 3초동안 동일한 라벨이어야 해당 라벨로 인정
                labelarray.removeAt(0)
                firstlabel = labelarray.get(0)
                secondlabel = labelarray.get(1)
                thirdlabel = labelarray.get(2)

                if(firstlabel == secondlabel && firstlabel == thirdlabel && firstlabel.isNotEmpty())
                    samelabeltest = true
                Log.d("labelarraytest", "$samelabeltest : $secondlabel")
                if(samelabeltest && !secondlabel.equals("") && !dialogcheck && !faildialog) {
                    labelteststatus = true
                    startdialog(secondlabel)
                }
            }
        }

        // 타이머 작동
        timerTask = timer(period = 100) {
            time++
            val sec = time / 15
            Log.d("sec", sec.toString())
            if(sec > 10 && labelarray.size > 3 && !samelabeltest && !faildialog) {
                timerTask?.cancel()
                Log.d("인식 실패 결과 호출", "타이머 3")
                faildialog = true
                val customDialog = CustomDialog(this@MainActivity)
                runOnUiThread {
                    customDialog.callFunction();
                }
                Log.d("TimerTask 작동 중지", "")
            }
        }



        // 인식 실패 결과 호출할 타이머
        val t3: TimerTask = object : TimerTask() {
            override fun run() {
                if(labelarray.size > 3 && !samelabeltest && !faildialog) {
                    Log.d("인식 실패 결과 호출", "타이머 3")
                    faildialog = true
                    val customDialog = CustomDialog(this@MainActivity)
                    runOnUiThread {
                        customDialog.callFunction();
                    }
                }
            }
        }

        // 라벨이 잡혔을 때
        val t4: TimerTask = object : TimerTask() {
            override fun run() {
                if(!secondlabel.equals("") && !thirdlabel.equals("") && !isSpeak && !dialogcheck) {
                    isSpeak = true
                    nearbyclothes()
                }
            }
        }


        val timer = Timer()
        timer.schedule(t0, 1000, 1000)
        timer.schedule(t1, 5000, 1000)
       // timer.schedule(t3, 1000, 10000)
        timer.schedule(t4, 3000, 1000)

    }

    // 라벨이 잡혔을 때 - 근처에 있다고 재생
    private fun nearbyclothes() {
        if(isSpeak) {
            speakOut("옷이 근처에 있습니다. 천천히 핸드폰을 움직여주세요.")
        }
    }


    // 의류 정보 다이얼로그 -> 추후 커스텀으로 변경
    private fun startdialog(label: String) {
        if(labelteststatus && !dialogcheck) {
            labelteststatus = false
            dialogcheck = true
            runOnUiThread {
                dialogstr = "촬영한 의류는 $label 입니다."
                var builder = AlertDialog.Builder(this@MainActivity)
                builder.setTitle("의류 정보")
                builder.setMessage("$dialogstr")
                builder.setIcon(R.mipmap.icon)
                builder.setPositiveButton("확인") { dialogstr, i ->
                    Log.d("다이얼로그 확인", "OK")
                    Handler().postDelayed({
                        dialogcheck = false
                    }, 5000) // 5초 정도 딜레이를 준 후 시작
                }
                builder.show()
                speakOut(dialogstr)
            }
        }
    }


    private fun startCamera() {
        //Implementation of preview useCase
        val previewConfig = PreviewConfig.Builder().apply {
            setTargetResolution(Size(viewFinder.width, viewFinder.height))
        }.build()

        val preview = Preview(previewConfig)

        preview.setOnPreviewOutputUpdateListener {
            var parent = viewFinder.parent as ViewGroup
            parent.removeView(viewFinder)
            parent.addView(viewFinder, 0)
            // viewFinder.surfaceTexture = it.surfaceTexture
            viewFinder.setSurfaceTexture(it.surfaceTexture)
            updateTransform()
        }

        //Implementation of preview useCase
        val analyzerConfig = ImageAnalysisConfig.Builder().apply {
            setImageReaderMode(
                ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE
            )
        }.build()

        val imageAnalyzer = ImageAnalyze(applicationContext)
        // Display inference results
        imageAnalyzer.setOnAnalyzeListener(object : ImageAnalyze.OnAnalyzeListener {
            override fun getAnalyzeResult(inferredCategory: String, score: Float) {
                // Change the view from other than the main thread
                viewFinder.post {
                    inferredCategoryText.text = "$inferredCategory"
                    inferredScoreText.text = "Score: $score"
                }
                currentCate = inferredCategory
            }
        })
        val analyzerUseCase = ImageAnalysis(analyzerConfig).apply {
            setAnalyzer(executor, imageAnalyzer)
        }

        //useCase is preview and image analysis
        CameraX.bindToLifecycle(this, preview, analyzerUseCase)
    }

    private fun updateTransform() {
        val matrix = Matrix()
        val centerX = viewFinder.width / 2f
        val centerY = viewFinder.height / 2f

        val rotationDegrees = when (viewFinder.display.rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> return
        }
        matrix.postRotate(-rotationDegrees.toFloat(), centerX, centerY)

        //Reflected in textureView
        viewFinder.setTransform(matrix)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                viewFinder.post { startCamera() }
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }



    private fun initTextToSpeech(){
        tts = TextToSpeech(this) {
            if(it == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED){
                    Toast.makeText(this, "Language not supported", Toast.LENGTH_SHORT).show()
                    return@TextToSpeech
                }
            }
        }
    }

    private fun speakOut(dialogstr1: String) {
        tts = TextToSpeech(applicationContext, TextToSpeech.OnInitListener {
            if(it == TextToSpeech.SUCCESS) {
                tts.language = Locale.KOREAN
                tts.setSpeechRate(1.0f)
                tts.speak(dialogstr1, TextToSpeech.QUEUE_ADD, null)
            }
        })
    }





}


