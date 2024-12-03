package com.example.limpialoya.Interfaces;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.example.limpialoya.R;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutionException;

public class CameraCaptureUI extends AppCompatActivity {

    private PreviewView previewView;
    private ImageCapture imageCapture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_capture);

        previewView = findViewById(R.id.previewView);
        Button btnTomarFoto = findViewById(R.id.btnTomarFoto);

        startCamera();

        btnTomarFoto.setOnClickListener(v -> capturePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error al abrir la cámara", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void capturePhoto() {
        if (imageCapture == null) return;

        imageCapture.takePicture(ContextCompat.getMainExecutor(this), new ImageCapture.OnImageCapturedCallback() {
            @Override
            public void onCaptureSuccess(@NonNull ImageProxy image) {
                Bitmap bitmap = imageProxyToBitmap(image);
                image.close();

                // Guardar el bitmap en un archivo temporal y obtener su URI
                Uri imageUri = saveImageToFile(bitmap);
                if (imageUri != null) {
                    Intent resultIntent = new Intent();
                    resultIntent.putExtra("capturedImageUri", imageUri.toString());
                    setResult(RESULT_OK, resultIntent);
                    finish();
                } else {
                    Toast.makeText(CameraCaptureUI.this, "Error al guardar la imagen", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Toast.makeText(CameraCaptureUI.this, "Error al capturar la foto", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Bitmap imageProxyToBitmap(ImageProxy imageProxy) {
        ByteBuffer buffer = imageProxy.getPlanes()[0].getBuffer();
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private Uri saveImageToFile(Bitmap bitmap) {
        try {
            // Reducir el tamaño de la imagen para una resolución más baja
            int reducedWidth = bitmap.getWidth() / 3;  // Cambia este valor según el nivel de reducción deseado
            int reducedHeight = bitmap.getHeight() / 3;

            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, reducedWidth, reducedHeight, true);

            // Crear un archivo temporal para almacenar la imagen redimensionada
            File outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            File imageFile = File.createTempFile("captured_image_", ".jpg", outputDir);

            FileOutputStream fos = new FileOutputStream(imageFile);
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 10, fos);  // Comprimir a una calidad baja para optimizar tamaño
            fos.close();

            // Liberar memoria de la imagen escalada
            scaledBitmap.recycle();

            return FileProvider.getUriForFile(this, "com.example.recogeloya.fileprovider", imageFile);

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
