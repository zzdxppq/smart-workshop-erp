package com.btsheng.erp.production.integration;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.core.model.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 字典 · erp-production → erp-platform
 * V1.3.9 · 设备类型等数据字典统一读取
 */
@FeignClient(
        name = "erp-platform",
        url = "${app.platform.url:}",
        path = "/dicts",
        contextId = "dictFeignClient")
public interface DictFeignClient {

    @GetMapping
    Result<List<Dict>> listByType(@RequestParam("type") String type);

    @GetMapping("/{id}")
    Result<Dict> getById(@PathVariable("id") Long id);
}
