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
class CustomDialogFail(context: Context) {
    private val context: Context = context
    lateinit var tts: TextToSpeech

    interface ButtonClickListener {
        fun onClicked(status: Int)
    }

    private lateinit var onClickedListener: ButtonClickListener
    fun setOnClickedListener(listener: ButtonClickListener): Unit {
        this.onClickedListener = listener
    }


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
        val closebtn: ImageButton = dlg.findViewById<View>(R.id.failure_close_btn) as ImageButton
        speakOut("인식에 실패했습니다. 다시 촬영하시겠습니까? 다시 촬영하시려면 화면을 짧게, 앱을 종료하시려면 길게 터치해주세요.")

        // 짧게 터치하면 다이얼로그만 종료 -> 카메라로 돌아감
        failuredialog.setOnClickListener{
            onClickedListener.onClicked(1) // 다이얼로그 처리 완료
            tts.stop()
            dlg.dismiss()
        }

        // 길게 터치하면 걍 다 종료
        failuredialog.setOnLongClickListener(View.OnLongClickListener {
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