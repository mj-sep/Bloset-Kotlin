package com.example.blosetk

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
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
import java.io.File
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import kotlin.concurrent.timer


private const val REQUEST_CODE_PERMISSIONS = 10
private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
private const val REQUEST_EXTERNAL_STORAGE = 1
private val PERMISSIONS_STORAGE = arrayOf(
    Manifest.permission.READ_EXTERNAL_STORAGE,
    Manifest.permission.WRITE_EXTERNAL_STORAGE
)


class MainActivity : AppCompatActivity() {
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var viewFinder: TextureView
    private lateinit var inferredCategoryText: TextView
    private lateinit var inferredScoreText: TextView
    private lateinit var activateCameraBtn: Button
    var labelarray = ArrayList<String>() // 같은 라벨인지 확인하기 위한 ArrayList
    lateinit var currentCate: String
    var samelabeltest: Boolean = false // 같은 라벨인지 확인
    var labelteststatus: Boolean = false // 테스트 진행중인지
    var isSpeak: Int = 0 // 음성 재생 중 아님 - 0, 재생 중 - 1

    var firstlabel: String = ""
    var secondlabel: String = ""
    var thirdlabel: String = ""

    var dialogcheck : Int = 0 // 다이얼로그 실행중 아님 - 0, 실행 중 - 1
    var faildialog : Boolean = false // 인식 실패 다이얼로그 실행중인지, 아님 - 0, 실행 중 -1
    var takescreen : Boolean = false // 사진 촬영 안함 - 0, 함 - 1


    private var tts: TextToSpeech? = null
    private var savedUri: Uri? = null

    // 타이머 관련
    private var time = 0
    private var timerTask : Timer? =null
    private var time2 = 0
    private var timerTask2 : Timer? =null
    private var timerTask3 : Timer? =null



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

        verifyStoragePermissions(this)

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
                Log.d("labellabellabel", samelabeltest.toString() + " , " + !secondlabel.equals("") + ", " + dialogcheck + ", " + !faildialog)
                if(samelabeltest && !secondlabel.equals("") && !thirdlabel.equals("") && dialogcheck == 0 && !faildialog) {
                    Log.d("labelarraystatus", labelteststatus.toString())
                    labelteststatus = true
                    // screenshot(viewFinder)
                    // takeScreenshot()
                    // takeScreenShot(getWindow().getDecorView());
                    takescreen = true
                }
            }
        }


        // 타이머 작동
        timerTask = timer(period = 1000) {
            time++
            val sec = time
            val secdivide = sec % 30
            Log.d("failure", "" + secdivide +
                    ", !samelabeltest : " + !samelabeltest + ", !faildialog: " + !faildialog)
                if (secdivide == 0 && labelarray.size > 3 && !samelabeltest && !faildialog) {
                    // timerTask?.cancel()
                    Log.d("인식 실패 결과 호출", "타이머 3")
                    faildialog = true
                    val customDialog = CustomDialogFail(this@MainActivity)
                    runOnUiThread {
                        customDialog.callFunction();
                        // 값을 넘겨 받아야 여기서 faildialog false로 변경해줄 수 있음.
                        customDialog.setOnClickedListener(object :
                            CustomDialogFail.ButtonClickListener {
                            override fun onClicked(status: Int) {
                                faildialog = false

                                Log.d("다이얼로그 정지 후 faildialog 값 변경", "true")
                            }
                        })
                    }
                    // Log.d("TimerTask 작동 중지", "")
            }
        }


        // 인식 실패 결과 호출할 타이머
        val t3: TimerTask = object : TimerTask() {
            override fun run() {
                if(labelarray.size > 2 && !samelabeltest && !faildialog) {
                    Log.d("인식 실패 결과 호출", "타이머 3")
                    faildialog = true
                    val customDialog = CustomDialogFail(this@MainActivity)
                    runOnUiThread {
                        customDialog.callFunction();
                    }
                }
            }
        }

        // 라벨이 잡혔을 때
        val t4: TimerTask = object : TimerTask() {
            override fun run() {
                Log.d("nearby", isSpeak.toString() + " , " + dialogcheck.toString())
                if(!secondlabel.equals("") && !thirdlabel.equals("") && isSpeak == 0 && dialogcheck == 0) {
                    isSpeak = 1
                    Log.d("nearbylabel", "true")
                    nearbyclothes()
                    tts?.playSilence(5000, TextToSpeech.QUEUE_ADD, null)
                }
            }
        }


        // 20초에 1번씩 근처 탐지 작동
        timerTask2 = timer(period = 1000) {
            time2++
            val dividetime2 = time2 % 20
            if(dividetime2 == 0) {
                isSpeak = 0
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
        speakOut("옷이 근처에 있습니다. 천천히 핸드폰을 움직여주세요.")
        Log.d("nearspeakout", "true")
    }




    // 의류 정보 다이얼로그 -> 추후 커스텀으로 변경
    private fun startdialog(label: String) {
        val customDialog2 = CustomDialogSuccess(this@MainActivity)
        if(labelteststatus && dialogcheck == 0) {
            dialogcheck = 1
            runOnUiThread {
                savedUri?.let { customDialog2.callFunction(label, it) };
                // 값을 넘겨 받아야 여기서 faildialog false로 변경해줄 수 있음.
                customDialog2.setOnClickedListener(object: CustomDialogSuccess.ButtonClickListener {
                    override fun onClicked(status: Int) {
                        labelteststatus = false
                        dialogcheck = 0
                        samelabeltest = false
                        Log.d("다이얼로그 정지 후 labelteststatus, dialogcheck 값 변경", "false")
                    }
                })
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

        //사진찍기 설정 시작
        val imageCaptureConfig = ImageCaptureConfig.Builder()
            .apply {
                setCaptureMode(ImageCapture.CaptureMode.MIN_LATENCY)

            }.build()

        val imageCapture = ImageCapture(imageCaptureConfig)

        val file = File(externalMediaDirs.first(), "${System.currentTimeMillis()}.jpg")


        timerTask3 = timer(period = 100) {
           if(takescreen) {
               takescreen = false
               imageCapture.takePicture(file, executor, object : ImageCapture.OnImageSavedListener {
                   override fun onError(
                       error: ImageCapture.ImageCaptureError,
                       message: String,
                       exc: Throwable?
                   ) {
                       val msg = "Photo capture failed: $message"
                       // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                       Log.e("CameraXApp", msg)
                       exc?.printStackTrace()
                   }

                   override fun onImageSaved(file: File) {
                       savedUri = Uri.fromFile(file)
                       val msg = "사진 경로 : ${file.absolutePath}"
                       // Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                       Log.d("CameraXApp", msg)
                       Log.d("savedUri", savedUri.toString())
                       startdialog(secondlabel)
                   }
               })
           }
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
        CameraX.bindToLifecycle(this, preview, imageCapture, analyzerUseCase)
    }




    /*
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // 사진 저장 장소
        val photoFile = File(
            outputDirectory,
            SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpg"
        )

        // outputFile의 Configuration을 담당
        val outputOptions = ImageCapture
            .OutputFileOptions
            .Builder(photoFile)
            .build()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                // 사진을 찍고 어떻게 저장할 지에 대한 구현부
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val message = "Photo capture succeeded: $savedUri"
                    Toast.makeText(applicationContext, message, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, message)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exception.message}", exception)
                }
            }
        )
    }
     */
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

        // TextureView에 반영
        viewFinder.setTransform(matrix)
    }


    // 카메라 허용
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
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            Toast.makeText(this, "SDK 버전이 낮습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        tts = TextToSpeech(this){
            if(it == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.KOREAN)
                if(result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Toast.makeText(this, "지원하지 않는 언어입니다.", Toast.LENGTH_SHORT).show()
                    return@TextToSpeech
                }
                Toast.makeText(this, "TTS 세팅 완료", Toast.LENGTH_SHORT)
            } else {
                Toast.makeText(this, "TTS 초기화에 실패했음.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun speakOut1(strTTS: String) {
        tts?.speak(strTTS, TextToSpeech.QUEUE_ADD, null, null)
    }

    private fun speakOut(strTTS: String) {
        tts = TextToSpeech(this, TextToSpeech.OnInitListener {
            if(it == TextToSpeech.SUCCESS) {
                tts?.language = Locale.KOREAN
                tts?.setSpeechRate(1.0f)
                tts?.speak(strTTS, TextToSpeech.QUEUE_ADD, null)
            }
        })
    }

    // 저장소 권한 부여
    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }

}


