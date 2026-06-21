package com.btsheng.erp.platform.dict.service;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.core.dict.entity.DictType;
import com.btsheng.erp.core.model.PageResponse;
import com.btsheng.erp.core.model.Result;
import com.btsheng.erp.platform.dict.mapper.DictMapper;
import com.btsheng.erp.platform.dict.mapper.DictTypeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

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

    private static final Set<String> BUILTIN_TYPES = Set.of(
            "MATERIAL_CATEGORY", "PROCESS_TYPE", "SURFACE_TREATMENT",
            "WORK_SHIFT", "WAREHOUSE", "CURRENCY", "MACHINE_TYPE"
    );

    public Result<List<DictType>> listTypes() {
        List<DictType> types = dictTypeMapper.selectList(null);
        return Result.ok(types);
    }

    public Result<DictType> createType(DictType type) {
        DictType existing = dictTypeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DictType>()
                        .eq(DictType::getTypeCode, type.getTypeCode()));
        if (existing != null) {
            return Result.fail(40908, "DICT_TYPE_CODE_DUPLICATE");
        }
        type.setStatus("ACTIVE");
        type.setIsBuiltin(0);
        dictTypeMapper.insert(type);
        return Result.ok(type);
    }

    public Result<DictType> updateType(String typeCode, DictType type) {
        DictType existing = dictTypeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DictType>()
                        .eq(DictType::getTypeCode, typeCode));
        if (existing == null) {
            return Result.fail(40401, "DICT_TYPE_NOT_FOUND");
        }
        existing.setTypeName(type.getTypeName());
        existing.setDescription(type.getDescription());
        dictTypeMapper.updateById(existing);
        return Result.ok(existing);
    }

    public Result<Void> deleteType(String typeCode) {
        if (BUILTIN_TYPES.contains(typeCode)) {
            return Result.fail(40909, "BUILTIN_DICT_TYPE");
        }
        DictType existing = dictTypeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<DictType>()
                        .eq(DictType::getTypeCode, typeCode));
        if (existing == null) {
            return Result.fail(40401, "DICT_TYPE_NOT_FOUND");
        }
        // 删除该类型下所有字典项
        dictMapper.delete(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Dict>()
                .eq(Dict::getDictType, typeCode));
        dictTypeMapper.deleteById(existing.getId());
        return Result.ok();
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
        if (BUILTIN_TYPES.contains(t)) {
            return Result.fail(40909, "BUILTIN_DICT");
        }
        existing.setStatus("DELETED");
        dictMapper.updateById(existing);
        return Result.ok();
    }
}
