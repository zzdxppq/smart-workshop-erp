package com.btsheng.erp.business.crm.warehouselocation.service;

import com.btsheng.erp.business.crm.quote.service.DocNoGenerator;
import com.btsheng.erp.business.crm.warehouselocation.dto.StocktakeCreateRequest;
import com.btsheng.erp.business.crm.warehouselocation.entity.CrmWarehouseStocktake;
import com.btsheng.erp.business.crm.warehouselocation.mapper.CrmWarehouseStocktakeMapper;
import com.btsheng.erp.core.model.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class StocktakeService {

    private final CrmWarehouseStocktakeMapper stocktakeMapper;
    private final DocNoGenerator docNoGenerator;

    @Autowired
    public StocktakeService(CrmWarehouseStocktakeMapper stocktakeMapper,
                            DocNoGenerator docNoGenerator) {
        this.stocktakeMapper = stocktakeMapper;
        this.docNoGenerator = docNoGenerator;
    }

    public Result<Map<String, Object>> listStocktakes() {
        List<CrmWarehouseStocktake> rows = stocktakeMapper.selectRecent();
        Map<String, Object> data = new HashMap<>();
        data.put("list", rows);
        data.put("items", rows);
        data.put("total", rows.size());
        return Result.ok(data);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<CrmWarehouseStocktake> createStocktake(StocktakeCreateRequest req, Long userId) {
        String warehouseCode = req != null && req.getWarehouseCode() != null && !req.getWarehouseCode().isBlank()
                ? req.getWarehouseCode().trim()
                : "WH-01";

        CrmWarehouseStocktake entity = new CrmWarehouseStocktake();
        entity.setStocktakeNo(docNoGenerator.nextStocktakeNo());
        entity.setWarehouseCode(warehouseCode);
        entity.setStatus("DRAFT");
        entity.setCreatedBy(userId);
        entity.setCreatedAt(LocalDateTime.now());
        stocktakeMapper.insert(entity);
        return Result.ok(entity);
    }
}
