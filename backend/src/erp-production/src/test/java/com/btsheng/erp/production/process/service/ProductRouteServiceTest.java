package com.btsheng.erp.production.process.service;

import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.production.integration.BusinessMaterialRouteGateway;
import com.btsheng.erp.production.material.entity.CrmMaterial;
import com.btsheng.erp.production.material.entity.MdmProcess;
import com.btsheng.erp.production.material.entity.MdmProductRoute;
import com.btsheng.erp.production.process.dto.ProductRouteCreateRequest;
import com.btsheng.erp.production.process.mapper.CrmProcessRouteMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProductRouteServiceTest {

    @Mock private BusinessMaterialRouteGateway businessGateway;
    @Mock private ProcessService processService;
    @Mock private CrmProcessRouteMapper routeMapper;

    private ProductRouteService service;

    @BeforeEach
    void setUp() {
        service = new ProductRouteService(businessGateway, processService, routeMapper);
    }

    @Test
    void create_product_route_success() {
        CrmMaterial material = new CrmMaterial();
        material.setId(8L);
        material.setMaterialCode("CP-0001");
        material.setMaterialName("齿轮减速机");
        when(businessGateway.resolveMaterial("8")).thenReturn(material);

        MdmProcess p01 = new MdmProcess();
        p01.setProcessCode("P01");
        p01.setProcessName("下料");
        p01.setStdTimeMin(new BigDecimal("30"));
        p01.setMachineType("CUT");
        p01.setUnitPrice(new BigDecimal("10"));
        when(businessGateway.getMdmProcess("P01")).thenReturn(p01);

        CrmProcess created = new CrmProcess();
        created.setId(100L);
        created.setProcessCode("PROC-20260615-0001");
        when(processService.createProcess(any(), eq(1L))).thenReturn(Result.ok(created));
        when(businessGateway.listRoutes("CP-0001")).thenReturn(List.of());

        ProductRouteCreateRequest req = new ProductRouteCreateRequest();
        ProductRouteCreateRequest.RouteProcessInput item = new ProductRouteCreateRequest.RouteProcessInput();
        item.setProcessSeq(1);
        item.setProcessCode("P01");
        req.setProcesses(List.of(item));

        Result<Map<String, Object>> r = service.createProductRoute("8", req, 1L);
        assertEquals(0, r.getCode());
        assertEquals(100L, r.getData().get("processId"));
        verify(businessGateway).updateMaterialProcessId(8L, 100L);
        verify(businessGateway).replaceRoutes(eq("CP-0001"), anyList());
    }

    @Test
    void copy_from_requires_source_route() {
        CrmMaterial target = new CrmMaterial();
        target.setId(9L);
        target.setMaterialCode("CP-0002");
        target.setMaterialName("WPA");
        CrmMaterial source = new CrmMaterial();
        source.setId(8L);
        source.setMaterialCode("CP-0001");
        source.setProcessId(null);
        when(businessGateway.resolveMaterial("9")).thenReturn(target);
        when(businessGateway.resolveMaterial("8")).thenReturn(source);

        Result<Map<String, Object>> r = service.copyProductRouteFrom("9", "8", 1L);
        assertNotEquals(0, r.getCode());
    }
}
