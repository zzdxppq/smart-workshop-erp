package com.btsheng.erp.production;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * erp-production · 车间执行服务 (工单/工序/扫码/报工/委外/设备)
 *
 * Port: 8083
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@EnableDiscoveryClient
@EnableFeignClients(basePackages = {"com.btsheng.erp.production.integration.client", "com.btsheng.erp.production.integration"})
@EnableCaching
@EnableScheduling
@MapperScan("com.btsheng.erp.production.**.mapper")
@SpringBootApplication(scanBasePackages = {
    "com.btsheng.erp.production",
    "com.btsheng.erp.core"
})
public class ErpProductionApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpProductionApplication.class, args);
        System.out.println("[ErpProductionApplication] started on port 8083");
    }
}
