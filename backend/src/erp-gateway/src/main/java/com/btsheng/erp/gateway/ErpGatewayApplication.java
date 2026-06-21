package com.btsheng.erp.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * erp-gateway · API 统一入口（8080 · Spring Cloud Gateway 转发 platform/business/production）
 */
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = "com.btsheng.erp.gateway")
public class ErpGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ErpGatewayApplication.class, args);
        System.out.println("[ErpGatewayApplication] started on port 8080 (gateway routes)");
    }
}
