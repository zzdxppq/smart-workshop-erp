package com.btsheng.erp.platform.label.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 二维码生成器（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.2 / AC-12.3.3）
 *
 * <p>基于 ZXing 3.5.x 渲染 QR Code PNG · 错误纠正级别 M（QR v3-M 上限 200 字符）
 * <p>QR 内容**纯文本** · 不加密不编码 · APP 扫码壳按前缀路由（architect §2.3）
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component
public class QrCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(QrCodeGenerator.class);

    /** 默认 QR 像素 · 与 layout_json.qrSizePx 默认值一致 */
    public static final int DEFAULT_QR_SIZE_PX = 300;

    /** 字符集 · UTF-8（兼容中文与符号） */
    public static final String CHAR_UTF_8 = "UTF-8";

    /** QR v3-M 上限 · 超出抛 42201（architect R6） */
    public static final int QR_V3M_MAX_LENGTH = 200;

    /**
     * 生成 QR Code PNG 字节数组
     *
     * @param content 二维码内容（纯文本）
     * @param sizePx  像素尺寸（layout_json.qrSizePx · 默认 300）
     * @return PNG 字节数组
     */
    public byte[] renderPng(String content, int sizePx) {
        validateContent(content);
        try {
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, CHAR_UTF_8);
            hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
            hints.put(EncodeHintType.MARGIN, 1); // 1 模块白边
            BitMatrix matrix = new MultiFormatWriter()
                    .encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            byte[] png = out.toByteArray();
            log.debug("[QrCodeGenerator] content='{}' sizePx={} bytes={}", content, sizePx, png.length);
            return png;
        } catch (WriterException | IOException e) {
            log.error("[QrCodeGenerator] 渲染失败 · content='{}'", content, e);
            throw new IllegalStateException("QR 渲染失败: " + e.getMessage(), e);
        }
    }

    /**
     * 默认 300×300 PNG
     */
    public byte[] renderPng(String content) {
        return renderPng(content, DEFAULT_QR_SIZE_PX);
    }

    private void validateContent(String content) {
        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("qr_content 不能为空");
        }
        if (content.length() > QR_V3M_MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "qr_content 长度超限 · " + content.length() + " > " + QR_V3M_MAX_LENGTH);
        }
    }
}