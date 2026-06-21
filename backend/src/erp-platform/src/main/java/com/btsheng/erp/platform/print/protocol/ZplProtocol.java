package com.btsheng.erp.platform.print.protocol;

import com.btsheng.erp.platform.print.dto.LabelData;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * ZPL II 协议渲染器（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.1）
 *
 * <p>适配 Zebra ZD420（ZPL 子集）
 * <p>布局：50mm × 30mm 标签
 * <p>每张：^XA 起始 + ^BCN QR + ^A0N 文本 + ^FO 定位 + ^XZ 结束
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component("ZPL")
public class ZplProtocol implements LabelProtocol {

    @Override
    public byte[] render(LabelData data, int copies) {
        StringBuilder sb = new StringBuilder();
        String colorBar = data.getColorBarHex() != null ? data.getColorBarHex() : "#1E40AF";
        List<String> lines = data.getLines() == null ? java.util.Collections.emptyList() : data.getLines();

        for (int i = 0; i < copies; i++) {
            sb.append("^XA");
            // 色条（顶部 8mm 高度 · 50mm 宽）
            sb.append("^FO20,15^GB380,40,40^FS");
            // QR Code（^BCN,80,Y,N,N - Code 128, magnification=80, 错误检测=Yes）
            sb.append("^FO20,70^BCN,80,Y,N,N^FD").append(safe(data.getQrContent())).append("^FS");
            // 文本行（最多 6 行 · 从 y=160 起每行 30 单位）
            for (int j = 0; j < Math.min(lines.size(), 6); j++) {
                int yPos = 160 + j * 30;
                String fontSize = (j == 0) ? "A0N,28,28" : "A0N,24,24";
                sb.append("^FO220,").append(yPos).append("^").append(fontSize)
                  .append("^FD").append(safe(lines.get(j))).append("^FS");
            }
            sb.append("^XZ");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String protocolName() {
        return "ZPL";
    }

    /**
     * ZPL 字段转义：^ / ~ 需双写
     */
    private static String safe(String s) {
        if (s == null) return "";
        return s.replace("^", "^^").replace("~", "~~");
    }
}
