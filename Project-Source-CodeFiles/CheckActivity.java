package com.example.plant;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;


import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.Objects;

public class CheckActivity extends AppCompatActivity {
    private static final int IMAGE_PICK_CODE = 1000;
    private static final int CAMERA_REQUEST_CODE = 1001;

    private ImageView imageView;
    private TextView resultText;
    private Interpreter tflite;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check);
        Button buttonCamera = findViewById(R.id.button_camera);
        Button buttonGallery = findViewById(R.id.button_gallery);
        Button buttonDetect = findViewById(R.id.button_detect);
        Button buttonLogout = findViewById(R.id.button_logout);
        imageView = findViewById(R.id.image_view);
        resultText = findViewById(R.id.result_text);

        try {
            tflite = new Interpreter(loadModelFile("plant_disease_model.tflite"));
        } catch (Exception e) {
            Log.e("TFLite", "Error loading model", e);
        }
        buttonCamera.setOnClickListener(v -> openCamera());
        buttonGallery.setOnClickListener(v -> pickImageFromGallery());
        buttonDetect.setOnClickListener(v -> detectDisease());
        buttonLogout.setOnClickListener(v -> logout());

    }
    private void openCamera() {
        // Logic to open camera and capture image
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        startActivityForResult(cameraIntent, CAMERA_REQUEST_CODE);
    }
    private void pickImageFromGallery() {
        // Logic to pick image from gallery
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Bitmap bitmap = null;
            if (requestCode == IMAGE_PICK_CODE) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(Objects.requireNonNull(data.getData()));
                    bitmap = BitmapFactory.decodeStream(inputStream);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (requestCode == CAMERA_REQUEST_CODE) {
                bitmap = (Bitmap) Objects.requireNonNull(data.getExtras()).get("data");
            }
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
            }
        }
    }
    private void detectDisease() {
        // Run inference on the selected image
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteBuffer inputBuffer = preprocessBitmap(bitmap);

        float[][] output = new float[1][1];  // Assuming single output (confidence score)
        tflite.run(inputBuffer, output);

        // Display the result
        resultText.setText("Disease detected with confidence: " + output[0][0]);
    }
    private MappedByteBuffer loadModelFile(String modelPath) throws IOException {
        try (InputStream is = getAssets().open(modelPath);
             FileChannel channel = ((FileInputStream) is).getChannel()) {
            long startOffset = 0;
            long declaredLength = channel.size();
            return channel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
        }
    }
    private ByteBuffer preprocessBitmap(Bitmap bitmap) {
        // Resize and normalize bitmap as needed by the model
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
        ByteBuffer buffer = ByteBuffer.allocateDirect(4 * 224 * 224 * 3);
        buffer.order(ByteOrder.nativeOrder());

        int[] intValues = new int[224 * 224];
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.getWidth(), 0, 0, resizedBitmap.getWidth(), resizedBitmap.getHeight());
        int pixel = 0;
        for (int i = 0; i < 224; ++i) {
            for (int j = 0; j < 224; ++j) {
                final int val = intValues[pixel++];
                buffer.putFloat(((val >> 16) & 0xFF) / 255.0f);
                buffer.putFloat(((val >> 8) & 0xFF) / 255.0f);
                buffer.putFloat((val & 0xFF) / 255.0f);
            }
        }
        return buffer;
    }
    private void logout() {
        // Handle logout action
        Toast.makeText(this, "Logged out", Toast.LENGTH_SHORT).show();
    }

}