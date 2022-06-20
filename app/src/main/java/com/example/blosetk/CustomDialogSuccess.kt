package com.example.blosetk

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import java.util.*
import kotlin.system.exitProcess


// 인식 성공 다이얼로그
class CustomDialogSuccess(context: Context) {
    private val context: Context = context
    lateinit var tts: TextToSpeech
    private lateinit var bitmap: Bitmap
    private var pixelarray: ArrayList<String> = ArrayList()


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
        val pixel2 = bitmap.getPixel(bitmap.width/2 + 20, bitmap.height/2 + 20)
        val pixel3 = bitmap.getPixel(bitmap.width/2 - 20, bitmap.height/2 - 20)
        val pixel4 = bitmap.getPixel(bitmap.width/2 + 10, bitmap.height/2 + 30)
        val pixel5 = bitmap.getPixel(bitmap.width/2 - 10, bitmap.height/2 - 30)

        pixelarray.add(getColor(pixel))
        pixelarray.add(getColor(pixel2))
        pixelarray.add(getColor(pixel3))
        pixelarray.add(getColor(pixel4))
        pixelarray.add(getColor(pixel5))

        // val hex = Integer.toHexString(pixel)

        // 최종 헥사값
        // val pixel_hex = "#" + hex.substring(2 until 8)
        var parray = Array(10) {i->0} // 빨 주 노 초 파 보 검 흰
        for (i in 0..4) {
            var colortext = pixelarray.get(i)
            when (colortext) {
                "빨간색" -> parray[0]++
                "주황색" -> parray[1]++
                "노란색" -> parray[2]++
                "초록색" -> parray[3]++
                "하늘색" -> parray[4]++
                "파란색" -> parray[5]++
                "보라색" -> parray[6]++
                "분홍색" -> parray[7]++
                "검은색" -> parray[8]++
                "흰색" -> parray[9]++
            }
            Log.d("색상값 배열", "$i 번째 색상: $colortext")
        }

        var colorfreqresult = 0 // 최대빈도
        var colornameint = 0 // 최대빈도색상배열번호

        for (i in 0..9) {
            var colorfreq = parray[i]
            if(colorfreq > colorfreqresult) {
                colorfreqresult = parray[i]
                colornameint = i
            }

        }
        Log.d("색상값 - ", colornameint.toString())
        var colorname = coloridtoname(colornameint)

        successtext.setText("인식 결과, $colorname $text" + "로 추정됩니다.")
        Log.d("색상값 텍스트 적용", "")
        speakOut(successtext.text.toString() + "다시 촬영하시려면 화면을 짧게, 앱을 종료하시려면 길게 터치해주세요.")

        // 짧게 터치하면 다이얼로그만 종료 -> 카메라로 돌아감
        successdialog.setOnClickListener{
            onClickedListener.onClicked(1) // 다이얼로그 처리 완료
            tts.stop()
            dlg.dismiss()
        }

        // 길게 터치하면 앱 종료
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


    private fun getColor(pixel: Int): String {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        val hsv = FloatArray(3)
        var colorname = "색 인식 불가"

        Color.RGBToHSV(r,g,b, hsv)

        Log.d("hsv-h", hsv[0].toString())
        Log.d("hsv-s", hsv[1].toString())
        Log.d("hsv-v", hsv[2].toString())

        if(hsv[1] in 0.0..0.90 && hsv[2] in 0.0..0.30) {
            colorname = "검은색"
        } else if (hsv[1] in 0.0..0.10 && hsv[2] in 0.80..1.00) {
            colorname = "흰색"
        } else {
            if (hsv[0] in 0.0..20.0) {
                colorname = "빨간색"
            } else if(hsv[0] in 20.1..50.0) {
                colorname = "주황색"
            } else if(hsv[0] in 50.1..90.0) {
                colorname = "노란색"
            } else if(hsv[0] in 90.1..150.0) {
                colorname = "초록색"
            } else if(hsv[0] in 150.1..180.0) {
                colorname = "하늘색"
            } else if(hsv[0] in 180.1..230.0) {
                colorname = "파란색"
            } else if(hsv[0] in 230.1..290.0) {
                colorname = "보라색"
            } else if(hsv[0] in 290.1..330.0) {
                colorname = "분홍색"
            } else if(hsv[0] in 330.1..360.0) {
                colorname = "빨간색"
            }
        }
        return colorname
    }

    private fun coloridtoname(color: Int): String {
        var colorreturn = ""
        when (color) {
            0 -> colorreturn = "빨간색"
            1 -> colorreturn = "주황색"
            2 -> colorreturn = "노란색"
            3 -> colorreturn = "초록색"
            4 -> colorreturn = "하늘색"
            5 -> colorreturn = "파란색"
            6 -> colorreturn = "보라색"
            7 -> colorreturn = "분홍색"
            8 -> colorreturn = "검은색"
            9 -> colorreturn = "흰색"
        }
        Log.d("색상값 - 리턴 ", colorreturn)

        return colorreturn
    }

}