package com.btsheng.erp.platform.label.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.platform.dict.mapper.DictMapper;
import com.btsheng.erp.platform.font.FontProvider;
import com.btsheng.erp.platform.label.dto.LabelLayout;
import com.btsheng.erp.platform.label.dto.LabelPreviewRequest;
import com.btsheng.erp.platform.label.dto.LabelPreviewResponse;
import com.btsheng.erp.platform.label.dto.LabelTemplateDTO;
import com.btsheng.erp.platform.label.dto.UpdateLabelTemplateRequest;
import com.btsheng.erp.platform.label.entity.LabelTemplate;
import com.btsheng.erp.platform.label.mapper.LabelTemplateMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 标签模板 Service（V1.3.9 Sprint 12 · Story 12.3 · AC-12.3.1/12.3.2/12.3.3/12.3.4�? *
 * <p>核心职责�? * <ul>
 *   <li>{@link #listTemplates(String, Long)}：列�?4 模板（GD/LZ/WW/WL�?· SB 由代码层 fallback 注入</li>
 *   <li>{@link #getTemplate(String, Long)}：按 type 取模�?· SB- �?fallback（取 GD + 覆盖 color_strip�?/li>
 *   <li>{@link #preview(LabelPreviewRequest, Long)}：渲�?PNG base64 · 三区布局 + QR + 明文</li>
 * </ul>
 *
 * <p>SB- fallback 设计（architect §2.2 · R1）：
 * <ul>
 *   <li>Label_template �?SB 行不存在�? �?seed：GD/LZ/WW/WL�?/li>
 *   <li>{@code getTemplate("SB")} �?GD 模板 + 覆盖 color_strip=#6B7280 · 严格**�?*覆盖 color_strip</li>
 *   <li>不覆�?layout_json · 不覆�?dpi · 不覆�?factory_name · 不覆�?layout</li>
 * </ul>
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-14
 */
@Service
public class LabelTemplateService {

    private static final Logger log = LoggerFactory.getLogger(LabelTemplateService.class);
    private static final Pattern HEX_COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");
    private static final java.util.Set<String> EDITABLE_TYPES =
            java.util.Set.of("GD", "LZ", "WW", "WL");

    /** 模板 type �?显示�?/ 前缀 / QR 示例 */
    private static final Map<String, String[]> TEMPLATE_META = new HashMap<>();
    static {
        TEMPLATE_META.put("GD", new String[]{"工单码", "GD-", "GD-260614-001"});
        TEMPLATE_META.put("LZ", new String[]{"流转码", "LZ-", "LZ-260613-001-P03"});
        TEMPLATE_META.put("SB", new String[]{"设备码", "SB-", "SB-260614-001"});
        TEMPLATE_META.put("WW", new String[]{"委外单码", "WW-", "WW-260614-001"});
        TEMPLATE_META.put("WL", new String[]{"物料码", "WL-", "WL-260614-0001"});
    }

    private final LabelTemplateMapper templateMapper;
    private final QrCodeGenerator qrCodeGenerator;
    private final FontProvider fontProvider;
    private final DictMapper dictMapper;

    @Autowired
    public LabelTemplateService(LabelTemplateMapper templateMapper,
                                QrCodeGenerator qrCodeGenerator,
                                FontProvider fontProvider,
                                DictMapper dictMapper) {
        this.templateMapper = templateMapper;
        this.qrCodeGenerator = qrCodeGenerator;
        this.fontProvider = fontProvider;
        this.dictMapper = dictMapper;
        log.info("[LabelTemplateService] 初始化完�?· FontProvider 注入状�? {}",
                fontProvider != null ? "OK" : "NULL(fallback)");
    }

    /**
     * TC-12.3.2.1 / 2.2 �?列模板元数据
     *
     * <p>type 为空时返�?5 种（4 入库 + 1 SB- 由代码层注入）；type 不为空时返回单元素或 SB- fallback
     * <p>顶层 companyName 默认 "昆山佰泰胜精密加�?
     */
    public Result<Map<String, Object>> listTemplates(String type, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        List<LabelTemplateDTO> list = new ArrayList<>();

        if (type == null || type.isEmpty()) {
            // 返回 5 种（GD/LZ/WW/WL 入库 + SB 代码层注入）
            List<LabelTemplate> rows = templateMapper.selectAllEnabled(tenantId);
            for (LabelTemplate t : rows) {
                list.add(toDto(t, null));
            }
            list.add(buildSbFallbackDto(tenantId));
        } else {
            if (!TEMPLATE_META.containsKey(type)) {
                return Result.fail(40002, "type 不支持");
            }
            LabelTemplate t = getTemplate(type, tenantId);
            if (t != null) {
                String reuseFrom = LabelTemplate.TYPE_SB.equals(type) ? LabelTemplate.TYPE_GD : null;
                list.add(toDto(t, reuseFrom));
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("templates", list);
        data.put("companyName", resolveCompanyName(tenantId));
        return Result.ok(data);
    }

    /**
     * 管理员更新模板（GD/LZ/WW/WL · SB 由 GD 色条 fallback 不可单独入库）
     */
    public Result<LabelTemplateDTO> updateTemplate(String type, UpdateLabelTemplateRequest req, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        if (type == null || !EDITABLE_TYPES.contains(type)) {
            return Result.fail(40002, "type 不支持或 SB 请通过编辑 GD 模板间接维护");
        }
        if (req == null) {
            return Result.fail(Result.CODE_PARAM_MISSING, "请求体不能为空");
        }
        LabelTemplate row = templateMapper.selectRawByType(type, tenantId);
        if (row == null) {
            return Result.fail(Result.CODE_NOT_FOUND, "模板不存在");
        }
        if (req.getColorStrip() != null && !req.getColorStrip().isBlank()) {
            String color = req.getColorStrip().trim();
            if (!HEX_COLOR.matcher(color).matches()) {
                return Result.fail(Result.CODE_PARAM_FORMAT, "色条须为 #RRGGBB 格式");
            }
            row.setColorStrip(color);
        }
        if (req.getFactoryName() != null && !req.getFactoryName().isBlank()) {
            String name = req.getFactoryName().trim();
            if (name.length() > 20) {
                return Result.fail(42201, "厂名最多 20 字符");
            }
            row.setFactoryName(name);
        }
        if (req.getDpi() != null) {
            if (req.getDpi() != 203 && req.getDpi() != 300) {
                return Result.fail(Result.CODE_PARAM_FORMAT, "DPI 仅支持 203 或 300");
            }
            row.setDpi(req.getDpi());
        }
        if (req.getEnabled() != null) {
            row.setEnabled(req.getEnabled() == 0 ? 0 : 1);
        }
        row.setUpdatedAt(LocalDateTime.now());
        templateMapper.updateById(row);
        return Result.ok(toDto(row, null));
    }

    /**
     * TC-12.3.1.2 / 2.2 �?�?type 取模板（�?SB- fallback�?     *
     * <p>SB-：DB 不存�?· �?GD 模板 + 覆盖 color_strip=#6B7280 · 严格只覆�?color_strip
     */
    public LabelTemplate getTemplate(String type, Long tenantId) {
        if (tenantId == null) tenantId = 1L;
        LabelTemplate row = templateMapper.selectByType(type, tenantId);
        if (row != null) {
            return row;
        }
        // SB- fallback：取 GD 模板 + 覆盖 color_strip
            if (LabelTemplate.TYPE_SB.equals(type)) {
            LabelTemplate gd = templateMapper.selectByType(LabelTemplate.TYPE_GD, tenantId);
            if (gd == null) {
                log.warn("[LabelTemplateService] SB- fallback 失败：GD 模板不存�?· tenantId={}", tenantId);
                return null;
            }
            // 严格只覆�?color_strip · 不覆�?layout / dpi / factory_name
            gd.setColorStrip(LabelTemplate.SB_FALLBACK_COLOR);
            return gd;
        }
        return null;
    }

    /**
     * TC-12.3.2.3 / 2.4 �?渲染标签预览 PNG base64
     *
     * <p>输出 base64 PNG（QA 测例期望 < 5KB）�?三区布局 + ZXing QR + 明文 lines
     */
    public Result<LabelPreviewResponse> preview(LabelPreviewRequest req, Long tenantId) {
        // 入参校验
            if (req.getType() == null || !TEMPLATE_META.containsKey(req.getType())) {
            return Result.fail(40002, "type 不支持");
        }
        if (req.getData() == null || req.getData().getQrContent() == null
                || req.getData().getQrContent().isEmpty()) {
            return Result.fail(40001, "qr_content 必填");
        }
        if (req.getData().getQrContent().length() > QrCodeGenerator.QR_V3M_MAX_LENGTH) {
            return Result.fail(42201, "qr_content �?200 字符");
        }
        if (req.getData().getLines() != null && req.getData().getLines().size() > 6) {
            return Result.fail(40001, "lines 最多 6 行");
        }

        // 取模板（SB- fallback�?
            LabelTemplate template = getTemplate(req.getType(), tenantId);
        if (template == null) {
            return Result.fail(40401, "模板不存�?· type=" + req.getType());
        }

        // 厂名：请�?> 模板字段 > sys_dict > 默认
            String factoryName = req.getData().getFactoryName();
        if (factoryName == null || factoryName.isEmpty()) {
            factoryName = template.getFactoryName();
        }
        if (factoryName == null || factoryName.isEmpty()) {
            factoryName = resolveCompanyName(tenantId);
        }
        if (factoryName.length() > 20) {
            return Result.fail(42201, "factory_name �?20 字符");
        }
        template.setFactoryName(factoryName);

        // QR 渲染（ZXing�?
            byte[] qrPng;
        try {
            int qrSizePx = LabelPngRenderer.parseLayout(template.getLayoutJson()).resolveQrSizePx();
            qrPng = qrCodeGenerator.renderPng(req.getData().getQrContent(), qrSizePx);
        } catch (IllegalArgumentException e) {
            return Result.fail(42201, e.getMessage());
        } catch (Exception e) {
            log.error("[LabelTemplateService] QR 渲染失败", e);
            return Result.fail(50001, "QR 渲染失败");
        }

        // 三区 PNG 合成（Sprint 13 · 注入 FontProvider 使用思源黑体�?
            byte[] labelPng;
        try {
            labelPng = new LabelPngRenderer(fontProvider).render(template, qrPng, req.getData().getLines());
        } catch (IOException e) {
            log.error("[LabelTemplateService] PNG 合成失败", e);
            return Result.fail(50001, "PNG 合成失败");
        }

        String format = req.getFormat() == null ? "PNG" : req.getFormat().toUpperCase();
        if (!"PNG".equals(format)) {
            // 本期仅支�?PNG · PDF 留待 12.4 接入 OpenPDF
            return Result.fail(40001, "format 仅支�?PNG · PDF 留待 12.4 接入");
        }

        String base64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(labelPng);
        LabelPreviewResponse resp = LabelPreviewResponse.builder()
                .type(req.getType())
                .format(format)
                .base64(base64)
                .contentType("image/png")
                .sizeBytes(labelPng.length)
                .renderedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME))
                .build();
        return Result.ok(resp);
    }

    /**
     * 渲染方法（service 层）· �?controller 解�?· �?12.4 复用
     */
    public byte[] renderToBytes(String type, String qrContent, List<String> lines, Long tenantId) throws IOException {
        LabelTemplate template = getTemplate(type, tenantId);
        if (template == null) {
            throw new IllegalArgumentException("模板不存�?· type=" + type);
        }
        byte[] qrPng = qrCodeGenerator.renderPng(qrContent);
        return new LabelPngRenderer(fontProvider).render(template, qrPng, lines);
    }

    // ========== 内部工具 ==========
            private LabelTemplateDTO toDto(LabelTemplate t, String reuseFrom) {
        String[] meta = TEMPLATE_META.get(t.getType());
        return LabelTemplateDTO.builder()
                .type(t.getType())
                .name(meta != null ? meta[0] : t.getType())
                .prefix(meta != null ? meta[1] : t.getType() + "-")
                .colorStrip(t.getColorStrip())
                .reuseFrom(reuseFrom)
                .layout(LabelPngRenderer.parseLayout(t.getLayoutJson()))
                .dpi(t.getDpi())
                .enabled(t.getEnabled() != null && t.getEnabled() == 1)
                .factoryName(t.getFactoryName())
                .qrExample(meta != null ? meta[2] : "")
                .createdAt(t.getCreatedAt() != null ? t.getCreatedAt().toString() : null)
                .updatedAt(t.getUpdatedAt() != null ? t.getUpdatedAt().toString() : null)
                .build();
    }

    private LabelTemplateDTO buildSbFallbackDto(Long tenantId) {
        // SB- 由代码层构�?· 不查 DB
            LabelTemplate gd = templateMapper.selectByType(LabelTemplate.TYPE_GD, tenantId);
        if (gd == null) {
            return null;
        }
        String[] meta = TEMPLATE_META.get(LabelTemplate.TYPE_SB);
        return LabelTemplateDTO.builder()
                .type(LabelTemplate.TYPE_SB)
                .name(meta[0])
                .prefix(meta[1])
                .colorStrip(LabelTemplate.SB_FALLBACK_COLOR)
                .reuseFrom(LabelTemplate.TYPE_GD)
                .layout(LabelPngRenderer.parseLayout(gd.getLayoutJson()))
                .dpi(gd.getDpi())
                .enabled(true)
                .factoryName(gd.getFactoryName())
                .qrExample(meta[2])
                .createdAt(null)
                .updatedAt(null)
                .build();
    }

    /**
     * 厂名解析：sys_dict dict_type=COMPANY_NAME value · 默认 "昆山佰泰胜精密加�?
     * 本期 dictMapper 注入可在此扩�?· 当前实现默认值（避免 dict 表强依赖�?     */
    private String resolveCompanyName(Long tenantId) {
        try {
            List<Dict> items = dictMapper.selectActiveByType("COMPANY_NAME");
            if (items != null) {
                for (Dict d : items) {
                    if ("DEFAULT".equalsIgnoreCase(d.getDictCode()) || items.size() == 1) {
                        return d.getDictLabel();
                    }
                }
                if (!items.isEmpty()) {
                    return items.get(0).getDictLabel();
                }
            }
        } catch (Exception e) {
            log.debug("[LabelTemplateService] COMPANY_NAME dict lookup failed tenantId={}", tenantId, e);
        }
        return LabelTemplate.DEFAULT_FACTORY_NAME;
    }
}