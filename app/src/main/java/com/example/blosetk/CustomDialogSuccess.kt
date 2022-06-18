package com.example.blosetk

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color
import android.speech.tts.TextToSpeech
import android.view.View;
import android.view.Window;
import android.view.WindowManager
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import java.util.*
import kotlin.concurrent.timer
import kotlin.system.exitProcess


// 인식 성공 다이얼로그
class CustomDialogSuccess(context: Context) {
    private val context: Context = context
    lateinit var tts: TextToSpeech

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
    fun callFunction(text: String) {

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

        successtext.setText("흰색, $text 등 \n학습한 데이터를 바탕으로 분석된 결과입니다.")
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

}