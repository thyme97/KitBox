package com.kitbox.qrcode;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;

/**
 * 二维码生成与解码服务（基于 ZXing core，仅支持 QR 码）。
 */
public final class QrCodeService {

    private QrCodeService() {
    }

    /**
     * 生成二维码图片。
     *
     * @param content  内容（按 UTF-8 编码）
     * @param size     输出边长（像素）
     * @param ecLevel  纠错级别 L / M / Q / H
     * @param margin   静区边距（模块数）
     */
    public static BufferedImage encode(String content, int size, String ecLevel, int margin) throws Exception {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("内容为空");
        }
        Map<EncodeHintType, Object> hints = new EnumMap<>(EncodeHintType.class);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.valueOf(ecLevel));
        hints.put(EncodeHintType.MARGIN, margin);
        com.google.zxing.common.BitMatrix matrix = new QRCodeWriter()
                .encode(content, BarcodeFormat.QR_CODE, size, size, hints);
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        return image;
    }

    /** 从图片解码二维码内容（自动做白底合成，支持带透明通道的图片）。 */
    public static String decode(BufferedImage source) throws Exception {
        if (source == null) {
            throw new IllegalArgumentException("图片为空");
        }
        // 合成到白底，避免透明区域干扰识别
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g = image.createGraphics();
        g.drawImage(source, 0, 0, java.awt.Color.WHITE, null);
        g.dispose();

        int width = image.getWidth();
        int height = image.getHeight();
        int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        com.google.zxing.RGBLuminanceSource luminance = new com.google.zxing.RGBLuminanceSource(width, height, pixels);
        BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(luminance));
        Map<DecodeHintType, Object> hints = new EnumMap<>(DecodeHintType.class);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
        try {
            Result result = new MultiFormatReader().decode(bitmap, hints);
            return result.getText();
        } catch (com.google.zxing.NotFoundException e) {
            throw new IllegalArgumentException("未在图片中识别到二维码");
        }
    }
}
