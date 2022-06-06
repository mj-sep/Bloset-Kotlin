package com.example.blosetk

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
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
    var firstlabel: String = ""
    var dialogstr : String = "" // 다이얼로그 문장
    lateinit var tts: TextToSpeech



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
                if(firstlabel == labelarray.get(1) && firstlabel == labelarray.get(2))
                    samelabeltest = true
                Log.d("labelarraytest", "$samelabeltest : $firstlabel")
                if(samelabeltest == true && !firstlabel.equals("")) labelteststatus = true
            }
        }

        val t2: TimerTask = object : TimerTask() {
            override fun run() {
                // labelteststatus가 true이면 타이머 중지 후 팝업 다이얼로그 띄우기
                if(labelteststatus == true) {
                    labelteststatus = false
                    t0.cancel()
                    t1.cancel()
                    startdialog(firstlabel)
                }
            }
        }


        val timer = Timer()
        timer.schedule(t0, 1000, 1000)
        timer.schedule(t1, 5000, 1000)
        timer.schedule(t2, 5000, 1000)

    }

    // 의류 정보 다이얼로그 -> 추후 커스텀으로 변경
    private fun startdialog(firstlabel: String) {
        runOnUiThread {
            dialogstr = "촬영한 의류는 $firstlabel 입니다."
            var builder = AlertDialog.Builder(this@MainActivity)
            builder.setTitle("의류 정보")
            builder.setMessage("$dialogstr")
            builder.setIcon(R.mipmap.ic_launcher)
            builder.show()
            speakOut(dialogstr)
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
                    inferredCategoryText.text = "인식 결과는 $inferredCategory 입니다."
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


