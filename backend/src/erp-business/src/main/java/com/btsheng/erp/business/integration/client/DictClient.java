package com.btsheng.erp.business.integration.client;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/** 字典 · erp-business → erp-platform */
@FeignClient(
        name = "erp-platform",
        url = "${app.platform.url:}",
        path = "/dicts",
        contextId = "dictClient")
public interface DictClient {

    @GetMapping
    Result<List<Dict>> listByType(@RequestParam("type") String type);

    @GetMapping("/{id}")
    Result<Dict> getById(@PathVariable("id") Long id);
}
