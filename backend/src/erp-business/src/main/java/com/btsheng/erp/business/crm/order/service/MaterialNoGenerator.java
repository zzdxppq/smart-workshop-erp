package com.btsheng.erp.business.crm.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.crm.order.entity.CrmOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * V2.1 · 报价与订单协同设计 · 料号生成器
 *
 * <p>核心规则：
 * - 报价阶段不生成料号
 * - 订单提交时生成/复用料号
 * - 料号编码：WL-{分类前缀}-{流水号} 或 WL-{流水号}
 * - 同一图号再次下单时复用已有料号
 */
@Component
public class MaterialNoGenerator {

    private final CrmMaterialMapper materialMapper;

    // 内存缓存：当日的料号序列（按分类前缀隔离）
    private final ConcurrentHashMap<String, AtomicLong> dailySeq = new ConcurrentHashMap<>();

    @Autowired
    public MaterialNoGenerator(CrmMaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    /**
     * 料号生成结果
     */
    public static class MaterialNoResult {
        private final String drawingNo;        // 图号
        private final String materialNo;       // 料号
        private final boolean isNew;           // 是否新建
        private final String materialName;     // 物料名称

        public MaterialNoResult(String drawingNo, String materialNo, boolean isNew, String materialName) {
            this.drawingNo = drawingNo;
            this.materialNo = materialNo;
            this.isNew = isNew;
            this.materialName = materialName;
        }

        public String getDrawingNo() { return drawingNo; }
        public String getMaterialNo() { return materialNo; }
        public boolean isNew() { return isNew; }
        public String getMaterialName() { return materialName; }
    }

    /**
     * 为订单明细行生成/复用料号
     *
     * @param items 订单明细列表
     * @param orderNo 订单号（用于记录首次生成来源）
     * @param operatorUserId 操作人ID
     * @return 每个明细行的料号生成结果
     */
    public List<MaterialNoResult> generateMaterialNos(List<CrmOrderItem> items, String orderNo, Long operatorUserId) {
        List<MaterialNoResult> results = new ArrayList<>();

        for (CrmOrderItem item : items) {
            String drawingNo = item.getDrawingNo();
            if (drawingNo == null || drawingNo.isBlank()) {
                // 无图号，跳过
                continue;
            }

            // 1. 查询该图号是否已有料号
            CrmMaterial existing = findMaterialByDrawingNo(drawingNo);
            String materialNo;
            String materialName;
            boolean isNew;

            if (existing != null) {
                // 2a. 已有料号，直接复用
                materialNo = existing.getMaterialCode();
                materialName = existing.getMaterialName();
                isNew = false;
            } else {
                // 2b. 无已有料号，生成新料号
                materialNo = generateNewMaterialNo(drawingNo);
                materialName = item.getMaterial() != null ? item.getMaterial() : drawingNo;
                isNew = true;

                // 写入物料主数据
                CrmMaterial newMaterial = new CrmMaterial();
                newMaterial.setMaterialCode(materialNo);
                newMaterial.setMaterialName(materialName);
                newMaterial.setSpec(item.getSpec());
                newMaterial.setDrawingNo(drawingNo);
                newMaterial.setGeneratedFromOrder(orderNo);
                newMaterial.setOwnerUserId(operatorUserId);
                newMaterial.setIsActive(1);
                materialMapper.insert(newMaterial);
            }

            // 3. 将料号写入订单明细行
            item.setMaterialNo(materialNo);

            results.add(new MaterialNoResult(drawingNo, materialNo, isNew, materialName));
        }

        return results;
    }

    /**
     * 查询物料主数据中是否已存在该图号
     */
    private CrmMaterial findMaterialByDrawingNo(String drawingNo) {
        return materialMapper.selectByDrawingNo(drawingNo);
    }

    /**
     * 生成新的料号
     * 格式：WL-{分类前缀}-{流水号} 或 WL-{流水号}
     */
    private String generateNewMaterialNo(String drawingNo) {
        // 根据图号分类前缀决定料号前缀
        // DWG-20260620-0001 -> 提取日期部分，生成 WL-日期-序号
        String prefix = "WL";
        if (drawingNo != null && drawingNo.startsWith("DWG-")) {
            // 图号格式：DWG-YYYYMMDD-NNNN
            // 料号格式：WL-YYYYMMDD-NNNN
            String datePart = drawingNo.substring(4, 12);  // 提取日期部分
            String key = datePart;
            AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
                long existing = countExistingWithPrefix(datePart);
                return new AtomicLong(existing + 1);
            });
            long n = seq.getAndIncrement();
            return String.format("WL-%s-%04d", datePart, n);
        }

        // 兜底：使用时间戳+序号
        String today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String key = today;
        AtomicLong seq = dailySeq.computeIfAbsent(key, k -> {
            long existing = countExistingWithPrefix(today);
            return new AtomicLong(existing + 1);
        });
        long n = seq.getAndIncrement();
        return String.format("WL-%s-%04d", today, n);
    }

    /**
     * 统计数据库中已存在的同前缀料号数量
     */
    private long countExistingWithPrefix(String prefix) {
        try {
            LambdaQueryWrapper<CrmMaterial> wrapper = new LambdaQueryWrapper<>();
            wrapper.likeRight(CrmMaterial::getMaterialCode, "WL-" + prefix)
                   .or()
                   .likeRight(CrmMaterial::getMaterialCode, prefix);
            return materialMapper.selectCount(wrapper);
        } catch (Exception e) {
            return 0L;
        }
    }

    /**
     * 根据图号获取已有料号（查询用）
     */
    public String getExistingMaterialNo(String drawingNo) {
        CrmMaterial material = findMaterialByDrawingNo(drawingNo);
        return material != null ? material.getMaterialCode() : null;
    }

    /**
     * 批量检查图号是否有已有料号
     */
    public Map<String, String> checkExistingMaterialNos(List<String> drawingNos) {
        Map<String, String> result = new HashMap<>();
        if (drawingNos == null || drawingNos.isEmpty()) {
            return result;
        }

        LambdaQueryWrapper<CrmMaterial> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(CrmMaterial::getDrawingNo, drawingNos);
        List<CrmMaterial> materials = materialMapper.selectList(wrapper);

        for (CrmMaterial material : materials) {
            result.put(material.getDrawingNo(), material.getMaterialCode());
        }

        return result;
    }
}
