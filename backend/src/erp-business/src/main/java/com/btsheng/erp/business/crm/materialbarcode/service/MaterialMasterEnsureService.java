package com.btsheng.erp.business.crm.materialbarcode.service;

import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 图纸/工程转化创建时同步 crm_material，保证料号查询可命中。
 */
@Service
public class MaterialMasterEnsureService {

    private final CrmMaterialMapper materialMapper;

    @Autowired
    public MaterialMasterEnsureService(CrmMaterialMapper materialMapper) {
        this.materialMapper = materialMapper;
    }

    /**
     * 若物料主数据不存在则按图纸信息补建（幂等）。
     */
    public CrmMaterial ensureFromDrawing(String materialCode, String title) {
        if (materialCode == null || materialCode.isBlank()) {
            return null;
        }
        String code = materialCode.trim();
        CrmMaterial existing = materialMapper.selectByMaterialCode(code);
        if (existing != null) {
            return existing;
        }
        CrmMaterial m = new CrmMaterial();
        m.setMaterialCode(code);
        m.setMaterialName(title != null && !title.isBlank() ? title.trim() : code);
        m.setSpec("图纸关联");
        m.setUnit("个");
        m.setCategoryId(3L);
        m.setCreatedAt(LocalDateTime.now());
        m.setUpdatedAt(LocalDateTime.now());
        materialMapper.insert(m);
        return m;
    }
}
