package com.example.blosetk

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import android.os.Handler
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.PixelCopy
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.*
import java.util.*
import kotlin.system.exitProcess


// 인식 성공 다이얼로그
class CustomDialogSuccess(context: Context) {
    private val context: Context = context
    lateinit var tts: TextToSpeech
    private lateinit var bitmap: Bitmap

    private var time = 0
    private var timerTask : Timer?=null

    interface ButtonClickListener {
        fun onClicked(status: Int)
    }

    private lateinit var onClickedListener: ButtonClickListener
    fun setOnClickedListener(listener: ButtonClickListener): Unit {
        this.onClickedListener = listener
    }

    // 호출할 다이얼로그 함수를 정의한다.
    fun callFunction(text: String, url: Uri) {

        // 커스텀 다이얼로그를 정의하기위해 Dialog클래스를 생성한다.
        val dlg = Dialog(context)
        dlg?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        // 액티비티의 타이틀바를 숨긴다.
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 커스텀 다이얼로그의 레이아웃을 설정한다.
        dlg.setContentView(R.layout.custom_dialog)

        // 커스텀 다이얼로그를 노출한다.
        dlg.show()

        // 커스텀 다이얼로그의 각 위젯들을 정의한다.
        val successdialog: RelativeLayout = dlg.findViewById<View>(R.id.successdialog) as RelativeLayout
        val successtext: TextView = dlg.findViewById<View>(R.id.success_text) as TextView
        val clothingimg: ImageView = dlg.findViewById<View>(R.id.clothingimage) as ImageView

        // 이미지 분석 결과 다이얼로그에 띄움
        clothingimg.setImageURI(url)
        clothingimg.isDrawingCacheEnabled = true
        clothingimg.buildDrawingCache(true)

        // 이미지 정가운데 픽셀 색상값
        bitmap = BitmapFactory.decodeFile(url.path)
        val pixel = bitmap.getPixel(bitmap.width/2, bitmap.height/2)
        val hex = Integer.toHexString(pixel)

        // 최종 헥사값
        val pixel_hex = "#" + hex.substring(2 until 8)


        successtext.setText("$pixel_hex, $text 등 \n학습한 데이터를 바탕으로 분석된 결과입니다.")
        speakOut(successtext.text.toString() + "다시 촬영하시려면 화면을 짧게 터치해주세요. 앱을 종료하시려면 화면을 길게 터치해주세요.")

        // 짧게 터치하면 다이얼로그만 종료 -> 카메라로 돌아감
        successdialog.setOnClickListener{
            onClickedListener.onClicked(1) // 다이얼로그 처리 완료
            dlg.dismiss()
        }

        // 길게 터치하면 걍 다 종료
        successdialog.setOnLongClickListener(View.OnLongClickListener {
            dlg.dismiss()
            exitProcess(0)
        })
    }

    private fun speakOut(dialogstr1: String) {
        tts = TextToSpeech(context, TextToSpeech.OnInitListener {
            if(it == TextToSpeech.SUCCESS) {
                tts.language = Locale.KOREAN
                tts.setSpeechRate(1.0f)
                tts.speak(dialogstr1, TextToSpeech.QUEUE_ADD, null)
            }
        })
    }

    fun getBitmapFromView(view: View, activity: Activity, callback: (Bitmap) -> Unit) {
        activity.window?.let { window ->
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            val locationOfViewInWindow = IntArray(2)
            view.getLocationInWindow(locationOfViewInWindow)
            try {
                PixelCopy.request(window, Rect(locationOfViewInWindow[0], locationOfViewInWindow[1], locationOfViewInWindow[0] + view.width, locationOfViewInWindow[1] + view.height), bitmap, { copyResult ->
                    if (copyResult == PixelCopy.SUCCESS) {
                        callback(bitmap)
                    }
                    // possible to handle other result codes ...
                }, Handler())
            } catch (e: IllegalArgumentException) {
                // PixelCopy may throw IllegalArgumentException, make sure to handle it
                e.printStackTrace()
            }
        }
    }

}