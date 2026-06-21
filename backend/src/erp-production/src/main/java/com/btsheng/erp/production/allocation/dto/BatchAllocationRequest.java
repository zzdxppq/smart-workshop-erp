package com.btsheng.erp.production.allocation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "批量提交工序归属（生管 · 工单工序划分）")
public class BatchAllocationRequest {
    private Long workorderId;
    private List<Item> items;

    @Data
    public static class Item {
        private Integer processSeq;
        /** INHOUSE / OUTSOURCE */
        private String decision;
    }
}
