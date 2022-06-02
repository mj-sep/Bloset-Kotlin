package com.example.blosetk

import android.content.Context
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import org.pytorch.IValue
import org.pytorch.LiteModuleLoader
import org.pytorch.torchvision.TensorImageUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*


class ImageAnalyze(context: Context) : ImageAnalysis.Analyzer {

    private lateinit var listener: OnAnalyzeListener    //Custom listener for updating View
    private var lastAnalyzedTimestamp = 0L
    // 모바일 모듈 로딩
    private val resnet = LiteModuleLoader.load(getAssetFilePath(context, "deeplabv3_scripte.ptl"))
    //var module = Module.load(assetFilePath(this, "model.ptl"))


    interface OnAnalyzeListener {
        fun getAnalyzeResult(inferredCategory: String, score: Float)
    }

    override fun analyze(image: ImageProxy, rotationDegrees: Int) {
        val currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp - lastAnalyzedTimestamp >= 0.5) {  // 0.Infer every 5 seconds
            lastAnalyzedTimestamp = currentTimestamp

            //Convert to tensor(I checked the image format and found YUV_420_It was called 888)
            val inputTensor = TensorImageUtils.imageYUV420CenterCropToFloat32Tensor(
                image.image,
                rotationDegrees,
                224,
                224,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB
            )

            // 추론 실행
            // 메소드는 로드된 모듈의 forward 메소드를 실행하고 결과를 outputTensor로 가져옴
            val outputTensor = resnet.forward(IValue.from(inputTensor)).toTensor()
            val scores = outputTensor.dataAsFloatArray

            // 처리 결과 반환 (ImageNet 클래스를 포함하는 배열에서 예측된 클래스 이름을 검색)
            var maxScore = -Float.MAX_VALUE
            var maxScoreIdx = -1
            for (i in scores.indices) { //Get the index with the highest score
                if (scores[i] > maxScore) {
                    maxScore = scores[i]
                    maxScoreIdx = i
                }
            }

            // 범위 내의 카테고리 이름
            Log.d("score", Arrays.toString(scores));
            Log.d("maxscoreIdx", maxScoreIdx.toString())

            val inferredCategory = ImageNetClasses().IMAGENET_CLASSES[maxScoreIdx]
            listener.getAnalyzeResult(inferredCategory, maxScore)  //Update View
        }
    }

    // Function to get the path from the asset file
    private fun getAssetFilePath(context: Context, assetName: String): String {
        val file = File(context.filesDir, assetName)
        if (file.exists() && file.length() > 0) {
            return file.absolutePath
        }
        else {
            Log.d("getAssetFilePath Error", "getAssetFilePath Error");
        }
        context.assets.open(assetName).use { inputStream ->
            FileOutputStream(file).use { outputStream ->
                val buffer = ByteArray(4 * 1024)
                var read: Int
                while (inputStream.read(buffer).also { read = it } != -1) {
                    outputStream.write(buffer, 0, read)
                }
                outputStream.flush()
            }
            return file.absolutePath
        }
    }

    fun setOnAnalyzeListener(listener: OnAnalyzeListener){
        this.listener = listener
    }
}