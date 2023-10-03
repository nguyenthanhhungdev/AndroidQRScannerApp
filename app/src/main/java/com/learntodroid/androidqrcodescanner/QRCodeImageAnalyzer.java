package com.learntodroid.androidqrcodescanner;

import androidx.annotation.NonNull;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.ChecksumException;
import com.google.zxing.FormatException;
import com.google.zxing.NotFoundException;
import com.google.zxing.PlanarYUVLuminanceSource;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

import java.nio.ByteBuffer;

import static android.graphics.ImageFormat.YUV_420_888;
import static android.graphics.ImageFormat.YUV_422_888;
import static android.graphics.ImageFormat.YUV_444_888;

public class QRCodeImageAnalyzer implements ImageAnalysis.Analyzer {
    private QRCodeFoundListener listener;

    public QRCodeImageAnalyzer(QRCodeFoundListener listener) {
        this.listener = listener;
    }

    @Override
    public void analyze(@NonNull ImageProxy image) {
//        Kiểm tra định dạng
        if (image.getFormat() == YUV_420_888 || image.getFormat() == YUV_422_888 || image.getFormat() == YUV_444_888) {
//            Trính xuất hình ảnh thành 1 mảng byte
            ByteBuffer byteBuffer = image.getPlanes()[0].getBuffer();
            byte[] imageData = new byte[byteBuffer.capacity()];
            byteBuffer.get(imageData);
//           Tạo đối tượng PlanarYUVLuminanceSource để có thể sử dụng thư viện ZXing
            PlanarYUVLuminanceSource source = new PlanarYUVLuminanceSource(
              imageData,
              image.getWidth(), image.getHeight(),
              0, 0,
              image.getWidth(), image.getHeight(),
              false
            );

//           Tạo đối tượng BinaryBitmap từ PlanarYUVLuminanceSource đề quét mã QR
            BinaryBitmap binaryBitmap = new BinaryBitmap(new HybridBinarizer(source));

            try {
//                Sử dụng QRCodeMultiReader để quét mã QR từ BinaryBitmap
                Result result = new QRCodeMultiReader().decode(binaryBitmap);
                listener.onQRCodeFound(result.getText());
            } catch (FormatException | ChecksumException | NotFoundException e) {
                listener.qrCodeNotFound();
            }
        }

        image.close();
    }
}
