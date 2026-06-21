package com.btsheng.erp.production.scan.util;

import java.util.regex.Pattern;

/**
 * Spec 统一码前缀：GD- 工单 / LZ- 流转 / SB- 设备 / WL- 物料 / WW- 委外。
 */
public final class BarcodePrefixUtil {

    public static final Pattern WORKORDER = Pattern.compile("^GD-(\\d{8})-(\\d{4})$");
    public static final Pattern TRANSFER = Pattern.compile("^LZ-([A-Z0-9-]+)$");
    public static final Pattern EQUIPMENT = Pattern.compile("^SB-([A-Z0-9-]+)$");
    public static final Pattern MATERIAL = Pattern.compile("^WL-([A-Z0-9-]+)$");
    public static final Pattern OUTSOURCE = Pattern.compile("^WW-(\\d{8})-(\\d{4})$");

    private BarcodePrefixUtil() {}

    public static boolean isWorkorder(String code) {
        return code != null && WORKORDER.matcher(code.trim()).matches();
    }

    public static boolean isTransfer(String code) {
        return code != null && TRANSFER.matcher(code.trim()).matches();
    }

    public static boolean isEquipment(String code) {
        return code != null && EQUIPMENT.matcher(code.trim()).matches();
    }

    public static String normalizeWorkorderNo(String barcode) {
        return barcode == null ? null : barcode.trim();
    }
}
