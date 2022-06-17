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


// 인식 실패 다이얼로그
class CustomDialog(context: Context) {
    private val context: Context = context
    lateinit var tts: TextToSpeech

    private var time = 0
    private var timerTask :Timer?=null

    interface backlistener {
        fun onBack(status: Boolean)
    }

    private lateinit var onbackListener: backlistener


    // 호출할 다이얼로그 함수를 정의한다.
    fun callFunction() {

        // 커스텀 다이얼로그를 정의하기위해 Dialog클래스를 생성한다.
        val dlg = Dialog(context)
        dlg?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
        // 액티비티의 타이틀바를 숨긴다.
        dlg.requestWindowFeature(Window.FEATURE_NO_TITLE)

        // 커스텀 다이얼로그의 레이아웃을 설정한다.
        dlg.setContentView(R.layout.custom_dialog_fail)

        // 커스텀 다이얼로그를 노출한다.
        dlg.show()

        // 커스텀 다이얼로그의 각 위젯들을 정의한다.
        val failuredialog: RelativeLayout = dlg.findViewById<View>(R.id.failure_dialog) as RelativeLayout

        speakOut("인식에 실패했습니다. 다시 촬영하시겠습니까? 10초 후 메인 화면으로 넘어갑니다. 앱을 종료하시려면 화면을 터치해주세요.")

        // 타이머 작동
        timerTask = timer(period = 10) {
            time++
            val sec = time / 100
            if(sec > 20) {
                timerTask?.cancel()
                onbackListener.onBack(true)
                dlg.dismiss()
            }
        }

        // 확인 버튼 누르면
        failuredialog.setOnClickListener(View.OnClickListener {
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