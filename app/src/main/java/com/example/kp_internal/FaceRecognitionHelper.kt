package com.example.kp_internal;
import android.content.Context;
import org.tensorflow.lite.Interpreter;
import java.io.FileInputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class FaceRecognitionHelper(context: Context) {
    private val interpreter: Interpreter

    init {
        val model = loadModelFile(context, "mobile_face_net.tflite")
        interpreter = Interpreter(model)
    }

    private fun loadModelFile(context: Context, modelName: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(modelName)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    fun recognizeFace(input: Array<Array<FloatArray>>): FloatArray {
        val output = Array(1) { FloatArray(192) }
        interpreter.run(input, output)
        return output[0]
    }
}
