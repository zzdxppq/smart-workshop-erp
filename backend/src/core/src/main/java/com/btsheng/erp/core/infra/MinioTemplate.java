package com.btsheng.erp.core.infra;

import com.btsheng.erp.core.web.DekLoader;
import com.btsheng.erp.core.model.AesGcmUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;

/**
 * MinIO 模板占位（V1.3.6 加密版本）
 *
 * <p>上传前 AES-256-GCM 加密，下载时自动解密。本类为占位实现，
 * 完整实装（分片 / 断点续传 / 预签名 URL）在 Story 1.3 补充。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@Component
public class MinioTemplate {

    @Value("${app.minio.endpoint:http://127.0.0.1:9000}")
            private String endpoint;

    @Value("${app.minio.access-key:minio}")
    private String accessKey;

    @Value("${app.minio.secret-key:minio123}")
    private String secretKey;

    private MinioClient client() {
        return MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
    }

    public void putEncryptedObject(String bucket, String key, byte[] plain) throws Exception {
        byte[] blob = AesGcmUtil.encrypt(DekLoader.requireDek(), plain);
        client().putObject(PutObjectArgs.builder()
                .bucket(bucket).object(key)
                .stream(new ByteArrayInputStream(blob), blob.length, -1)
                .build());
    }

    public byte[] getDecryptedObject(String bucket, String key) throws Exception {
        byte[] blob = client().getObject(GetObjectArgs.builder().bucket(bucket).object(key).build())
                .readAllBytes();
        return AesGcmUtil.decrypt(DekLoader.requireDek(), blob);
    }
}
