package com.btsheng.erp.platform.print.protocol;

import com.btsheng.erp.platform.print.dto.LabelData;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * TSPL 协议渲染器（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.2/1.3）
 *
 * <p>适配 TSC TTP-244 Pro + 启邦 DL-888B（TSPL 同源）
 * <p>布局：50mm × 30mm 标签
 * <p>指令流：SIZE / CLS / QRCODE / TEXT / PRINT
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component("TSPL")
public class TsplProtocol implements LabelProtocol {

    @Override
    public byte[] render(LabelData data, int copies) {
        StringBuilder sb = new StringBuilder();
        List<String> lines = data.getLines() == null ? java.util.Collections.emptyList() : data.getLines();

        for (int i = 0; i < copies; i++) {
            sb.append("SIZE 50 mm,30 mm\r\n");
            sb.append("CLS\r\n");
            // QR Code 20,20 起始位置 · M=中等纠错 · 6=放大系数
            sb.append("QRCODE 20,20,M,6,A,0,\"").append(escape(data.getQrContent())).append("\"\r\n");
            // 文本行（最多 6 行 · 从 y=130 起每行 28 单位）
            for (int j = 0; j < Math.min(lines.size(), 6); j++) {
                int yPos = 130 + j * 28;
                // TEXT x,y,"font",rotation,x-mul,y-mul,"content"
            sb.append("TEXT 220,").append(yPos)
                  .append(",\"3\",0,1,1,\"").append(escape(lines.get(j))).append("\"\r\n");
            }
            // 份数：本协议不支持份数概念（由 copies 循环模拟）
            sb.append("PRINT 1\r\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String protocolName() {
        return "TSPL";
    }

    /**
     * TSPL 字符串字段转义：双引号需双写
     */
    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\"\"");
    }
}
