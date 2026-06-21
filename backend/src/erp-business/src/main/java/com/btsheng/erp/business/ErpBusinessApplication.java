package com.btsheng.erp.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * erp-business · 业务聚合服务 (CRM/销售/采购/仓储/品质/财务/人事/报表/对账/料号成本)
 *
 * Port: 8082
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.btsheng.erp.business.integration.client")
@EnableCaching  // V1.3.8 Sprint 7 集成 A：Story 2.1 详情聚合 + Story 4.3 总经理报表 Redis 5min 缓存
@org.springframework.scheduling.annotation.EnableScheduling
@MapperScan("com.btsheng.erp.business.**.mapper")
@SpringBootApplication(scanBasePackages = {
    "com.btsheng.erp.business",
    "com.btsheng.erp.core"
})
public class ErpBusinessApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpBusinessApplication.class, args);
        System.out.println("[ErpBusinessApplication] started on port 8082");
    }
}
