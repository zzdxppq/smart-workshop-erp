from pathlib import Path

p = Path(r"e:\claude\smart-workshop-erp\backend\src\erp-production\src\main\java\com\btsheng\erp\production\outsource\service\OutsourceService.java")
text = p.read_text(encoding="utf-8")
start = text.index("    public Result<List<BigDecimal>> getPriceHistory")
end = text.index("    // ====== 私有 ======", start)
new_block = """    public Result<OutsourceHistoryPriceResult> getHistoryPrice(Long vendorId, String processName) {
        if (vendorId == null) {
            return Result.fail(40001, "SUPPLIER_ID_REQUIRED");
        }
        if (processName == null || processName.isBlank()) {
            return Result.fail(40001, "PROCESS_NAME_REQUIRED");
        }
        List<BigDecimal> prices = orderMapper.selectRecentPrices(vendorId, processName.trim());
        return Result.ok(buildHistoryPriceResult(vendorId, processName.trim(), null, prices));
    }

    public Result<List<BigDecimal>> getPriceHistory(Long supplierId, String materialCode) {
        if (supplierId == null || materialCode == null || materialCode.isBlank()) {
            return Result.ok(List.of());
        }
        return Result.ok(orderMapper.selectRecentPricesByMaterial(supplierId, materialCode.trim()));
    }

    public Result<OutsourceHistoryPriceResult> getPriceSuggest(Long supplierId, String processName,
                                                                String materialCode) {
        if (supplierId == null) {
            return Result.fail(40001, "SUPPLIER_ID_REQUIRED");
        }
        String process = (processName != null && !processName.isBlank()) ? processName.trim() : null;
        String material = (materialCode != null && !materialCode.isBlank()) ? materialCode.trim() : null;
        if (process == null && material == null) {
            return Result.fail(40001, "PROCESS_OR_MATERIAL_REQUIRED");
        }
        List<BigDecimal> prices = process != null
                ? orderMapper.selectRecentPrices(supplierId, process)
                : orderMapper.selectRecentPricesByMaterial(supplierId, material);
        return Result.ok(buildHistoryPriceResult(supplierId, process, material, prices));
    }

    private OutsourceHistoryPriceResult buildHistoryPriceResult(Long vendorId, String processName,
                                                                 String materialCode,
                                                                 List<BigDecimal> prices) {
        if (prices == null || prices.isEmpty()) {
            return OutsourceHistoryPriceResult.empty(vendorId, processName);
        }
        List<BigDecimal> sorted = new ArrayList<>(prices);
        sorted.sort(BigDecimal::compareTo);
        BigDecimal median = sorted.get(sorted.size() / 2);
        BigDecimal sum = BigDecimal.ZERO;
        for (BigDecimal p : prices) {
            sum = sum.add(p);
        }
        OutsourceHistoryPriceResult result = new OutsourceHistoryPriceResult();
        result.setVendorId(vendorId);
        result.setProcessName(processName);
        result.setMaterialCode(materialCode);
        result.setHistoryPrices(prices);
        result.setSuggestedPrice(median);
        result.setAvgPrice(sum.divide(new BigDecimal(prices.size()), 2, RoundingMode.HALF_UP));
        result.setSampleCount(prices.size());
        result.setEmpty(false);
        return result;
    }

"""
text = text[:start] + new_block + text[end:]
import re
text = re.sub(r"\n    /\*\*[\s\S]*?P1 修补 3[\s\S]*?\*/\n", "\n", text, count=1)
p.write_text(text, encoding="utf-8")
print("patched OutsourceService")
