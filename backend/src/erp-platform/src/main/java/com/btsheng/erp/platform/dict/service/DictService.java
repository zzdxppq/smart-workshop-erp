package com.btsheng.erp.platform.dict.service;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.dict.mapper.DictMapper;
import com.btsheng.erp.platform.dict.mapper.DictTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/** platform 本地字典服务 · 读写 cnc_platform.sys_dict */
@Service
public class DictService {

    private final DictMapper dictMapper;
    private final DictTypeMapper dictTypeMapper;

    @Autowired
    public DictService(DictMapper dictMapper, DictTypeMapper dictTypeMapper) {
        this.dictMapper = dictMapper;
        this.dictTypeMapper = dictTypeMapper;
    }

    public Result<List<Dict>> listByType(String dictType) {
        if (dictType == null || dictType.isBlank()) {
            return Result.ok(dictMapper.selectActive(null));
        }
        return Result.ok(dictMapper.selectActiveByType(dictType));
    }

    public Result<PageResponse<Dict>> pageByType(String dictType, int pageNum, int pageSize) {
        int page = Math.max(pageNum, 1);
        int size = Math.max(pageSize, 1);
        List<Dict> all = dictMapper.selectActive(dictType);
        long total = all.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, all.size());
        List<Dict> slice = from >= all.size() ? List.of() : all.subList(from, to);
        return Result.ok(PageResponse.of(slice, total, page, size));
    }

    public Result<Dict> getById(Long id) {
        Dict d = dictMapper.selectById(id);
        if (d == null || "DELETED".equals(d.getStatus())) {
            return Result.fail(40401, "DICT_NOT_FOUND");
        }
        return Result.ok(d);
    }

    public Result<Dict> createDict(Dict dict) {
        if (dictMapper.countByTypeAndCode(dict.getDictType(), dict.getDictCode()) > 0) {
            return Result.fail(40908, "DICT_CODE_DUPLICATE");
        }
        dict.setStatus("ACTIVE");
        dictMapper.insert(dict);
        return Result.ok(dict);
    }

    public Result<Dict> updateDict(Long id, Dict dict) {
        Dict existing = dictMapper.selectById(id);
        if (existing == null || "DELETED".equals(existing.getStatus())) {
            return Result.fail(40401, "DICT_NOT_FOUND");
        }
        existing.setDictLabel(dict.getDictLabel());
        existing.setSort(dict.getSort());
        existing.setStatus(dict.getStatus());
        dictMapper.updateById(existing);
        return Result.ok(existing);
    }

    public Result<Void> deleteDict(Long id) {
        Dict existing = dictMapper.selectById(id);
        if (existing == null) {
            return Result.fail(40401, "DICT_NOT_FOUND");
        }
        String t = existing.getDictType();
        if ("MATERIAL_CATEGORY".equals(t) || "PROCESS_TYPE".equals(t)
                || "SURFACE_TREATMENT".equals(t) || "WORK_SHIFT".equals(t)
                || "WAREHOUSE".equals(t) || "CURRENCY".equals(t)) {
            return Result.fail(40909, "BUILTIN_DICT");
        }
        existing.setStatus("DELETED");
        dictMapper.updateById(existing);
        return Result.ok();
    }
}
