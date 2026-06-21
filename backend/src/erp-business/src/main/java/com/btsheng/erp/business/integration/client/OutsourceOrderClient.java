package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.integration.dto.OutsourceOrderRef;
import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/** 委外单 · erp-business → erp-production */
@FeignClient(
        name = "erp-production",
        url = "${app.production.url:}",
        path = "/outsource",
        contextId = "outsourceOrderClient")
public interface OutsourceOrderClient {

    @GetMapping("/id/{id}")
    Result<OutsourceOrderRef> getById(@PathVariable("id") Long id);
}
