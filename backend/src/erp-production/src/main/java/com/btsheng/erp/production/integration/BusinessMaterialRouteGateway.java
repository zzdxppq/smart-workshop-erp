package com.btsheng.erp.production.integration;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.integration.client.BusinessProductRouteClient;
import com.btsheng.erp.production.material.entity.CrmMaterial;
import com.btsheng.erp.production.material.entity.MdmProcess;
import com.btsheng.erp.production.material.entity.MdmProductRoute;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 封装 Feign 调用，供 ProductRouteService 使用 */
@Component
public class BusinessMaterialRouteGateway {

    private final BusinessProductRouteClient client;

    public BusinessMaterialRouteGateway(BusinessProductRouteClient client) {
        this.client = client;
    }

    public CrmMaterial resolveMaterial(String productId) {
        Result<Map<String, Object>> r = client.resolveMaterial(productId);
        if (r == null || !r.isSuccess() || r.getData() == null) {
            return null;
        }
        return mapMaterial(r.getData());
    }

    public MdmProcess getMdmProcess(String processCode) {
        Result<Map<String, Object>> r = client.getMdmProcess(processCode);
        if (r == null || !r.isSuccess() || r.getData() == null) {
            return null;
        }
        return mapMdmProcess(r.getData());
    }

    public List<MdmProductRoute> listRoutes(String productCode) {
        Result<List<Map<String, Object>>> r = client.listRoutes(productCode);
        if (r == null || !r.isSuccess() || r.getData() == null) {
            return List.of();
        }
        List<MdmProductRoute> out = new ArrayList<>();
        for (Map<String, Object> row : r.getData()) {
            out.add(mapRouteRow(productCode, row));
        }
        return out;
    }

    public void replaceRoutes(String productCode, List<MdmProductRoute> routes) {
        List<Map<String, Object>> body = new ArrayList<>();
        for (MdmProductRoute row : routes) {
            Map<String, Object> m = new HashMap<>();
            m.put("processSeq", row.getProcessSeq());
            m.put("processCode", row.getProcessCode());
            m.put("isOutsource", row.getIsOutsource());
            body.add(m);
        }
        client.replaceRoutes(productCode, body);
    }

    public void updateMaterialProcessId(Long materialId, Long processId) {
        Map<String, Object> body = new HashMap<>();
        body.put("processId", processId);
        client.updateProcessId(materialId, body);
    }

    private CrmMaterial mapMaterial(Map<String, Object> data) {
        CrmMaterial m = new CrmMaterial();
        m.setId(asLong(data.get("id")));
        m.setMaterialCode(asString(data.get("materialCode")));
        m.setMaterialName(asString(data.get("materialName")));
        m.setProcessId(asLong(data.get("processId")));
        m.setIsActive(asInt(data.get("isActive")));
        Object cost = data.get("costTotal");
        if (cost instanceof Number n) {
            m.setCostTotal(BigDecimal.valueOf(n.doubleValue()));
        }
        return m;
    }

    private MdmProcess mapMdmProcess(Map<String, Object> data) {
        MdmProcess p = new MdmProcess();
        p.setId(asLong(data.get("id")));
        p.setProcessCode(asString(data.get("processCode")));
        p.setProcessName(asString(data.get("processName")));
        Object std = data.get("stdTimeMin");
        if (std instanceof Number n) {
            p.setStdTimeMin(BigDecimal.valueOf(n.doubleValue()));
        }
        p.setMachineType(asString(data.get("machineType")));
        Object price = data.get("unitPrice");
        if (price instanceof Number n) {
            p.setUnitPrice(BigDecimal.valueOf(n.doubleValue()));
        }
        return p;
    }

    private MdmProductRoute mapRouteRow(String productCode, Map<String, Object> data) {
        MdmProductRoute row = new MdmProductRoute();
        row.setProductCode(productCode);
        row.setProcessSeq(asInt(data.get("processSeq")));
        row.setProcessCode(asString(data.get("processCode")));
        Object out = data.get("isOutsource");
        row.setIsOutsource(out instanceof Boolean b ? b : Boolean.TRUE.equals(out));
        return row;
    }

    private Long asLong(Object v) {
        return v instanceof Number n ? n.longValue() : null;
    }

    private Integer asInt(Object v) {
        return v instanceof Number n ? n.intValue() : null;
    }

    private String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }
}
