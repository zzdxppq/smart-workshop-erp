package com.btsheng.erp.platform.health;

import com.btsheng.erp.core.web.DekLoader;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * DEK 健康探针（V1.3.7 P1 修补 · 关键）
 *
 * <p>{@code /platform/health/dek}：返回 DEK 加载状态。
 * K8s liveness / 启动期探针不直接 500，而是返回 JSON 状态由上层决策。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Tag(name = "Platform-Health", description = "基础能力健康检查")
@RestController
@RequestMapping("/platform/health")
public class DekHealthController {

    @Operation(summary = "DEK 加载健康探针")
    @GetMapping("/dek")
    public Map<String, Object> dek() {
        Map<String, Object> r = new HashMap<>();
        r.put("ready", DekLoader.isReady());
        r.put("loadedFrom", DekLoader.getLoadedFrom());
        r.put("devFallback", DekLoader.isDevFallback());
        r.put("status", DekLoader.isReady() ? "UP" : "DOWN");
        r.put("message", DekLoader.isDevFallback()
                ? "DEK running in dev fallback mode (V1.3.6 生产环境严禁)"
                : "DEK loaded from production file");
        return r;
    }
}
