package com.btsheng.erp.business.internal.productroute.service;

import com.btsheng.erp.business.crm.materialbarcode.entity.CrmMaterial;
import com.btsheng.erp.business.crm.materialbarcode.mapper.CrmMaterialMapper;
import com.btsheng.erp.business.internal.productroute.dto.ProductRouteMaterialDto;
import com.btsheng.erp.business.internal.productroute.dto.ProductRouteRowDto;
import com.btsheng.erp.business.internal.productroute.dto.UpdateMaterialProcessRequest;
import com.btsheng.erp.business.internal.productroute.entity.MdmProcess;
import com.btsheng.erp.business.internal.productroute.entity.MdmProductRoute;
import com.btsheng.erp.business.internal.productroute.mapper.MdmProcessMapper;
import com.btsheng.erp.business.internal.productroute.mapper.MdmProductRouteMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 产品工艺路线 · cnc_business 侧数据（供 erp-production Feign 调用，替代 production 双数据源）
 */
@Service
public class ProductRouteMaterialInternalService {

    private final CrmMaterialMapper materialMapper;
    private final MdmProcessMapper mdmProcessMapper;
    private final MdmProductRouteMapper productRouteMapper;

    public ProductRouteMaterialInternalService(CrmMaterialMapper materialMapper,
                                               MdmProcessMapper mdmProcessMapper,
                                               MdmProductRouteMapper productRouteMapper) {
        this.materialMapper = materialMapper;
        this.mdmProcessMapper = mdmProcessMapper;
        this.productRouteMapper = productRouteMapper;
    }

    public Result<ProductRouteMaterialDto> resolveMaterial(String productId) {
        CrmMaterial material = findMaterial(productId);
        if (material == null) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        return Result.ok(toDto(material));
    }

    public Result<MdmProcess> getMdmProcess(String processCode) {
        MdmProcess mdm = mdmProcessMapper.selectByCode(processCode);
        if (mdm == null) {
            return Result.fail(40404, "MDM_PROCESS_NOT_FOUND");
        }
        return Result.ok(mdm);
    }

    public Result<List<ProductRouteRowDto>> listProductRoutes(String productCode) {
        List<MdmProductRoute> rows = productRouteMapper.selectByProductCode(productCode);
        return Result.ok(rows.stream().map(this::toRowDto).collect(Collectors.toList()));
    }

    @Transactional
    public Result<Void> replaceProductRoutes(String productCode, List<ProductRouteRowDto> routes) {
        productRouteMapper.deleteByProductCode(productCode);
        if (routes != null) {
            for (ProductRouteRowDto row : routes) {
                MdmProductRoute entity = new MdmProductRoute();
                entity.setProductCode(productCode);
                entity.setProcessSeq(row.getProcessSeq());
                entity.setProcessCode(row.getProcessCode());
                entity.setIsOutsource(Boolean.TRUE.equals(row.getIsOutsource()));
                productRouteMapper.insert(entity);
            }
        }
        return Result.ok(null);
    }

    @Transactional
    public Result<Void> updateMaterialProcessId(Long materialId, UpdateMaterialProcessRequest req) {
        CrmMaterial material = materialMapper.selectById(materialId);
        if (material == null || material.getIsActive() == null || material.getIsActive() != 1) {
            return Result.fail(40404, "PRODUCT_NOT_FOUND");
        }
        material.setProcessId(req.getProcessId());
        material.setUpdatedAt(LocalDateTime.now());
        materialMapper.updateById(material);
        return Result.ok(null);
    }

    private CrmMaterial findMaterial(String productId) {
        if (productId == null || productId.isBlank()) {
            return null;
        }
        try {
            Long id = Long.parseLong(productId.trim());
            CrmMaterial byId = materialMapper.selectActiveById(id);
            if (byId != null) {
                return byId;
            }
        } catch (NumberFormatException ignored) {
            // material_code
        }
        return materialMapper.selectByMaterialCode(productId.trim());
    }

    private ProductRouteMaterialDto toDto(CrmMaterial material) {
        ProductRouteMaterialDto dto = new ProductRouteMaterialDto();
        dto.setId(material.getId());
        dto.setMaterialCode(material.getMaterialCode());
        dto.setMaterialName(material.getMaterialName());
        dto.setProcessId(material.getProcessId());
        dto.setIsActive(material.getIsActive());
        dto.setCostTotal(material.getCostTotal());
        return dto;
    }

    private ProductRouteRowDto toRowDto(MdmProductRoute row) {
        ProductRouteRowDto dto = new ProductRouteRowDto();
        dto.setProcessSeq(row.getProcessSeq());
        dto.setProcessCode(row.getProcessCode());
        dto.setIsOutsource(row.getIsOutsource());
        return dto;
    }
}
