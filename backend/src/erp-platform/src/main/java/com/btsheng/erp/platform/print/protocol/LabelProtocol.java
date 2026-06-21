package com.btsheng.erp.platform.print.protocol;

import com.btsheng.erp.platform.print.dto.LabelData;

/**
 * 标签打印协议抽象（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1）
 *
 * <p>适配器模式：ZplProtocol / TsplProtocol 各自实现
 * <p>工厂按 {@code sys_printer.protocol} 字段注入
 * <p>型号映射：Zebra ZD420 → ZPL · TSC TTP-244 Pro + 启邦 DL-888B → TSPL
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
public interface LabelProtocol {

    /**
     * 根据 LabelData 渲染协议字节流（UTF-8）
     *
     * @param data 标签数据
     * @param copies 份数（ZPL 在 render 内部循环 N 次 ^XA...^XZ）
     * @return 协议字节流
     */
    byte[] render(LabelData data, int copies);

    /**
     * 协议名 · 与 sys_printer.protocol 字段对应
     */
    String protocolName();
}
