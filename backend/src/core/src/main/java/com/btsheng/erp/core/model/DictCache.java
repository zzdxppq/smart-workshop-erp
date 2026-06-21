package com.btsheng.erp.core.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 字典缓存（V1.3.7 · in-memory placeholder）
 *
 * <p>简化版：基于 ConcurrentHashMap 缓存 dict_type -> [items]。
 * 真实实装在 Story 1.3 接入 sys_dict 表 + Redis TTL 3600s + Stream dict-invalidate 失效。
 *
 * @author 河南晓评信息科技有限公司
 * @since 2026-06-10
 */
public class DictCache {

    private static final Logger log = LoggerFactory.getLogger(DictCache.class);
    private static final DictCache INSTANCE = new DictCache();
    private final Map<String, List<DictItem>> cache = new ConcurrentHashMap<>();

    public static DictCache getInstance() {
        return INSTANCE;
    }

    private DictCache() {
    }

    public void put(String dictType, List<DictItem> items) {
        cache.put(dictType, items == null ? Collections.emptyList() : List.copyOf(items));
        log.debug("[DictCache] put type={} size={}", dictType, items == null ? 0 : items.size());
    }

    public List<DictItem> get(String dictType) {
        return cache.getOrDefault(dictType, Collections.emptyList());
    }

    public void evict(String dictType) {
        cache.remove(dictType);
    }

    public void clear() {
        cache.clear();
    }

    /**
     * 字典项。
     */
    public static class DictItem {
        private String code;
        private String label;
        private int sort;

        public DictItem() {
        }

        public DictItem(String code, String label, int sort) {
            this.code = code;
            this.label = label;
            this.sort = sort;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }

        public int getSort() {
            return sort;
        }

        public void setSort(int sort) {
            this.sort = sort;
        }
    }
}
