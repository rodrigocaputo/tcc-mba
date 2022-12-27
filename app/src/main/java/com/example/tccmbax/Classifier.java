package com.example.tccmbax;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.media.Image;
import android.util.Log;

import androidx.camera.core.ImageProxy;

import com.example.tccmbax.ml.Model;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;

public class Classifier {
    private Context context;
    Interpreter tflite;
//    final String ASSOCIATED_AXIS_LABELS = "labels.txt";
    final String ASSOCIATED_AXIS_LABELS = "labels_model.txt";
    List<String> associatedAxisLabels = null;

    public Classifier(Context context) {
        this.context = context;
        try {
            associatedAxisLabels = FileUtil.loadLabels(context, ASSOCIATED_AXIS_LABELS);
        } catch (IOException e) {
            Log.e("tfliteSupport", "Error reading label file", e);
        }
//        try {
////            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(context,
////                    "mobilenet_v1_0.25_128_quantized_1_metadata_1.tflite");
////            tflite = new Interpreter(tfliteModel);
//        } catch (IOException e) {
//            Log.e("tfliteSupport", "Error reading model", e);
//        }
    }

    public String classify(ImageProxy image) {
        @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
        Image img = image.getImage();

        String result = " ";
        try {
            Model model = Model.newInstance(context);

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 320, 240, 1}, DataType.UINT8);

            ByteBuffer byteBuffer = Utils.toByteBuffer(img);


            inputFeature0.loadBuffer(byteBuffer);

            // Runs model inference and gets result.
            Model.Outputs outputs = model.process(inputFeature0);

            TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

//            Log.d("TCCMBAX",
//                    "\n" + outputFeature0.getFloatArray()[0]
//                    + "\n" + outputFeature0.getFloatArray()[1]
//                    + "\n" + outputFeature0.getFloatArray()[2]);

            result = String.format("center: %.2f", outputFeature0.getFloatArray()[0]) + "\n" +
                    String.format("right: %.2f", outputFeature0.getFloatArray()[1]) + "\n" + // INVERTI O SINAL PARA TESTAR
                    String.format("left: %.2f", outputFeature0.getFloatArray()[2]);

            if (outputFeature0.getFloatArray()[0] > outputFeature0.getFloatArray()[1] &&
                    outputFeature0.getFloatArray()[0] > outputFeature0.getFloatArray()[2]) {
                result = "C" + "\n" + result;
            } else if (outputFeature0.getFloatArray()[1] < outputFeature0.getFloatArray()[2]) { // INVERTI O SINAL PARA TESTAR
                result = "L" + "\n" + result;
            } else {
                result = "R" + "\n" + result;
            }

            // Releases model resources if no longer used.
            model.close();
        } catch (IOException e) {
            // TODO Handle the exception
        }
        return result;
    }

    public String classify2(ImageProxy image) {
        @SuppressLint({"UnsafeExperimentalUsageError", "UnsafeOptInUsageError"})
        Image img = image.getImage();
        Bitmap bitmap = Utils.toBitmap(img);
        int rotation = Utils.getImageRotation(image);
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = height > width ? width : height;

        ImageProcessor imageProcessor = new ImageProcessor.Builder()
                .add(new ResizeWithCropOrPadOp(size, size))
                .add(new ResizeOp(128, 128, ResizeOp.ResizeMethod.BILINEAR))
                .add(new Rot90Op(rotation))
                .build();
        TensorImage tensorImage = new TensorImage(DataType.UINT8);
        tensorImage.load(bitmap);
        tensorImage = imageProcessor.process(tensorImage);
        TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 1001}, DataType.UINT8);
        if (null != tflite) {
            tflite.run(tensorImage.getBuffer(), probabilityBuffer.getBuffer());
        }
        TensorProcessor probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(0, 255)).build();

        String result = " ";
        if (null != associatedAxisLabels) {
            // Map of labels and their corresponding probability
            TensorLabel labels = new TensorLabel(associatedAxisLabels, probabilityProcessor.process(probabilityBuffer));

            // Create a map to access the result based on label
            Map<String, Float> floatMap = labels.getMapWithFloatValue();
            result = Utils.writeResults(floatMap);
        }
        return result;
    }
}
