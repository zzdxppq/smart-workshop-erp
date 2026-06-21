package com.btsheng.erp.platform.dict.service;

import com.btsheng.erp.core.dict.entity.Dict;
import com.btsheng.erp.platform.dict.mapper.DictMapper;
import com.btsheng.erp.platform.dict.mapper.DictTypeMapper;
import com.btsheng.erp.platform.dict.service.DictService;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** V1.3.7 Story 1.3 · AC-1.3.1 · DictService 测例�? 测例 · 1.3-test-design §2.1�?*/
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DictServiceTest {

    @Mock private DictMapper dictMapper;
    @Mock private DictTypeMapper dictTypeMapper;

    @Test void list_by_type() {
        when(dictMapper.selectActiveByType("MATERIAL_CATEGORY")).thenReturn(List.of(
                new Dict(), new Dict(), new Dict(), new Dict(), new Dict()));
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Result<List<Dict>> r = svc.listByType("MATERIAL_CATEGORY");
        assertEquals(5, r.getData().size());
    }

    @Test void get_by_id_active() {
        Dict d = new Dict();
        d.setStatus("ACTIVE");
        when(dictMapper.selectById(1L)).thenReturn(d);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        assertEquals(0, svc.getById(1L).getCode());
    }

    @Test void get_by_id_deleted_40401() {
        Dict d = new Dict();
        d.setStatus("DELETED");
        when(dictMapper.selectById(1L)).thenReturn(d);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Result<Dict> r = svc.getById(1L);
        assertEquals(40401, r.getCode());
    }

    @Test void create_dict_duplicate_40908() {
        when(dictMapper.countByTypeAndCode("MATERIAL_CATEGORY", "STEEL")).thenReturn(1);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Dict d = new Dict();
        d.setDictType("MATERIAL_CATEGORY");
        d.setDictCode("STEEL");
        Result<Dict> r = svc.createDict(d);
        assertEquals(40908, r.getCode());
    }

    @Test void create_dict_success() {
        when(dictMapper.countByTypeAndCode(any(), any())).thenReturn(0);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Dict d = new Dict();
        d.setDictType("PROCESS_TYPE");
        d.setDictCode("CNC");
        d.setDictLabel("CNC 加工");
        Result<Dict> r = svc.createDict(d);
        assertEquals(0, r.getCode());
    }

    @Test void update_dict_not_found_40401() {
        when(dictMapper.selectById(99L)).thenReturn(null);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Result<Dict> r = svc.updateDict(99L, new Dict());
        assertEquals(40401, r.getCode());
    }

    @Test void delete_dict_not_found_40401() {
        when(dictMapper.selectById(99L)).thenReturn(null);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Result<Void> r = svc.deleteDict(99L);
        assertEquals(40401, r.getCode());
    }

    @Test void delete_dict_soft() {
        Dict d = new Dict();
        d.setDictType("CUSTOM_TYPE");
        d.setStatus("ACTIVE");
        when(dictMapper.selectById(1L)).thenReturn(d);
        DictService svc = new DictService(dictMapper, dictTypeMapper);
        Result<Void> r = svc.deleteDict(1L);
        assertEquals(0, r.getCode());
    }
}
