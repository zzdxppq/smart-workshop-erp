package com.btsheng.erp.core.web;

import com.btsheng.erp.core.model.AesGcmUtil;
import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;

import java.nio.charset.StandardCharsets;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * AES-256-GCM 字段 TypeHandler（V1.3.6/V1.3.7）
 *
 * <p>MyBatis 透明加解密：写入时 {@code [12B IV][密文+16B Tag]}；读取时自动解。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
@MappedTypes(String.class)
@MappedJdbcTypes(JdbcType.BINARY)
public class AesGcmTypeHandler extends BaseTypeHandler<String> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setBytes(i, null);
            return;
        }
        ps.setBytes(i, AesGcmUtil.encrypt(DekLoader.requireDek(), parameter.getBytes(StandardCharsets.UTF_8)));
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        byte[] blob = rs.getBytes(columnName);
        if (blob == null) return null;
        return decodeBlob(blob);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        byte[] blob = rs.getBytes(columnIndex);
        if (blob == null) return null;
        return decodeBlob(blob);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        byte[] blob = cs.getBytes(columnIndex);
        if (blob == null) return null;
        return decodeBlob(blob);
    }

    /** init_data 明文 JSON 与 AES 密文兼容 */
    private static String decodeBlob(byte[] blob) {
        if (blob.length > 0) {
            char first = (char) blob[0];
            if (first == '[' || first == '{') {
                return new String(blob, StandardCharsets.UTF_8);
            }
        }
        try {
            return AesGcmUtil.decryptToString(DekLoader.requireDek(), blob);
        } catch (RuntimeException ex) {
            String plain = new String(blob, StandardCharsets.UTF_8).trim();
            if (plain.startsWith("[") || plain.startsWith("{")) {
                return plain;
            }
            throw ex;
        }
    }
}
