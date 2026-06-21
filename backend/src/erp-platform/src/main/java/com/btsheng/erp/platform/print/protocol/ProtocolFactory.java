package com.btsheng.erp.platform.print.protocol;

import com.btsheng.erp.core.web.BizException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 协议工厂（V1.3.9 Sprint 12 · Story 12.4 · AC-12.4.1 · TC-12.4.1.4/1.6）
 *
 * <p>按 {@code sys_printer.protocol} 字段名注入
 * <p>Spring 自动注入所有 LabelProtocol bean · 用 protocolName() 作为 map key
 * <p>未注册协议 → 抛 BizException(50202, "PROTOCOL_UNSUPPORTED: " + name)
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Component
public class ProtocolFactory {

    private final Map<String, LabelProtocol> protocols;

    public ProtocolFactory(List<LabelProtocol> impls) {
        this.protocols = impls.stream()
                .collect(Collectors.toMap(LabelProtocol::protocolName, p -> p, (a, b) -> a));
    }

    /**
     * 获取协议渲染器
     *
     * @param name 协议名（ZPL / TSPL）
     * @return LabelProtocol
     * @throws BizException 50202 协议未注册
     */
    public LabelProtocol get(String name) {
        LabelProtocol p = protocols.get(name);
        if (p == null) {
            throw new BizException(50202, "PROTOCOL_UNSUPPORTED: " + name);
        }
        return p;
    }

    /**
     * 是否已注册
     */
    public boolean has(String name) {
        return protocols.containsKey(name);
    }

    /**
     * 已注册协议列表
     */
    public java.util.Set<String> registeredProtocols() {
        return protocols.keySet();
    }
}
