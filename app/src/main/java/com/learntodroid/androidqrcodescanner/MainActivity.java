package com.learntodroid.androidqrcodescanner;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST_CAMERA = 0;

    private PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;

    private Button qrCodeFoundButton;
    private String qrCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        previewView = findViewById(R.id.activity_main_previewView);

//        qrCodeFoundButton = findViewById(R.id.activity_main_qrCodeFoundButton);
//        qrCodeFoundButton.setVisibility(View.INVISIBLE);
//        qrCodeFoundButton.setOnClickListener(v -> {
//            Toast.makeText(getApplicationContext(), qrCode, Toast.LENGTH_SHORT).show();
//            Uri webPage = Uri.parse(qrCode);
//            Intent intentOpenWebPage = new Intent(Intent.ACTION_VIEW, webPage);
//            startActivity(intentOpenWebPage);
//            Log.i(MainActivity.class.getSimpleName(), "QR Code Found: " + qrCode);
//        });

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        requestCamera();
    }

    //    Yêu cầu quyền truy cập camera
    private void requestCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
//            if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
//            } else {
//                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
//            }
        }
    }

    //    Kiểm tra quyền truy cập camera được trả về từ requestCamera
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera Permission Denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    //    Khởi động camera
    private void startCamera() {
//        Lắng nghe quá trình khởi động camera hoàn tất
        cameraProviderFuture.addListener(() -> {
            try {
//                Lấy quyền truy cập vào camera thông qua ProcessCameraProvider
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error starting camera " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
//        Thiết lập chế độ hiền thị
        previewView.setImplementationMode(PreviewView.ImplementationMode.PERFORMANCE);

//        Tạo đối tượng hiển thị hình ảnh trực tiếp từ camera
        Preview preview = new Preview.Builder()
                .build();

//        Chọn cam sau
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

//        Tạo đối tượng xử lý hình ảnh
        ImageAnalysis imageAnalysis =
                new ImageAnalysis.Builder()
                        .setTargetResolution(new Size(1280, 720))
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) //Xác định có mã qr bên trong hình ảnh hay không
                        .build();

        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), new QRCodeImageAnalyzer(new QRCodeFoundListener() {
            @Override
            public void onQRCodeFound(String _qrCode) {
//                Tìm thấy mã qr code, gán mã và hiện nút
                qrCode = _qrCode;
                Toast.makeText(getApplicationContext(), qrCode, Toast.LENGTH_SHORT).show();
                Log.i(MainActivity.class.getSimpleName(), "QR Code Found: " + qrCode);
                if (qrCode.contains("@gmail.com")) {
                    sendEmail(qrCode);
                } else {
                    openWebPage(qrCode);
                }
//                qrCodeFoundButton.setVisibility(View.VISIBLE);
            }

            @Override
            public void qrCodeNotFound() {
//                qrCodeFoundButton.setVisibility(View.INVISIBLE);
                Toast.makeText(getApplicationContext(), "Không tìm thấy mã QR", Toast.LENGTH_SHORT).show();
            }
        }));

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageAnalysis, preview);
    }

    private void openWebPage(String url) {
        Uri webPage = Uri.parse(url);
        Intent intentOpenWebPage = new Intent(Intent.ACTION_VIEW, webPage);
        startActivity(intentOpenWebPage);
    }

    private void sendEmail(String uri) {
        Intent intent = new Intent(Intent.ACTION_SENDTO);

        String[] parts = uri.split(";");
        String to = parts[0].split(":")[2];
        String subject = parts[1].split(":")[1];
        String body = parts[2].split(":")[1];

        intent.setData(Uri.parse("mailto:" + to));
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        startActivity(intent);
    }
}