package com.btsheng.erp.business.crm.drawing.service;

import com.btsheng.erp.business.crm.drawing.dto.AttachmentDownloadPayload;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawing;
import com.btsheng.erp.business.crm.drawing.entity.CrmDrawingAttachment;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingAttachmentMapper;
import com.btsheng.erp.business.crm.drawing.mapper.CrmDrawingMapper;
import com.btsheng.erp.core.model.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * FR-3-2-2 · CAD/CAM 附件上传/列表/下载
 */
@ExtendWith(MockitoExtension.class)
class DrawingAttachmentServiceTest {

    @Mock private CrmDrawingMapper drawingMapper;
    @Mock private CrmDrawingAttachmentMapper attachmentMapper;
    @Mock private DrawingMinioFileService fileStorage;

    @InjectMocks
    private DrawingAttachmentService service;

    @TempDir
    Path tempDir;

    @Test
    void upload_and_download_cad_attachment() throws Exception {
        CrmDrawing drawing = new CrmDrawing();
        drawing.setId(42L);
        drawing.setDrawingNo("DWG-20260620-0001");
        when(drawingMapper.selectById(42L)).thenReturn(drawing);
        when(fileStorage.toMinioUri(anyString())).thenAnswer(inv -> "local://" + tempDir.resolve("cad/test.dxf"));
        when(attachmentMapper.insert(any(CrmDrawingAttachment.class))).thenAnswer(inv -> {
            CrmDrawingAttachment row = inv.getArgument(0);
            row.setId(900L);
            return 1;
        });

        MockMultipartFile file = new MockMultipartFile(
                "file", "fixture.dxf", "application/dxf", "SECTION\nHEADER\n".getBytes());
        Result<CrmDrawingAttachment> up = service.uploadAttachment(42L, file, 1001L);
        assertEquals(0, up.getCode());
        assertNotNull(up.getData());
        assertEquals("fixture.dxf", up.getData().getFileName());
        assertEquals("DXF", up.getData().getFileType());

        verify(fileStorage).putObject(contains("cad/42/"), any(), eq((long) file.getSize()), eq("application/dxf"));

        CrmDrawingAttachment stored = new CrmDrawingAttachment();
        stored.setId(900L);
        stored.setFileName("fixture.dxf");
        stored.setFilePath("local://" + tempDir.resolve("cad/test.dxf"));
        when(attachmentMapper.selectById(900L)).thenReturn(stored);
        Path localFile = tempDir.resolve("cad/test.dxf");
        Files.createDirectories(localFile.getParent());
        Files.writeString(localFile, "SECTION\nHEADER\n");
        when(fileStorage.readBytes(stored.getFilePath())).thenReturn(Files.readAllBytes(localFile));

        Result<AttachmentDownloadPayload> down = service.downloadAttachment(900L);
        assertEquals(0, down.getCode());
        assertNotNull(down.getData());
        assertTrue(down.getData().getData().length > 0);
    }

    @Test
    void list_attachments_for_drawing() {
        when(drawingMapper.selectById(1L)).thenReturn(new CrmDrawing());
        CrmDrawingAttachment a = new CrmDrawingAttachment();
        a.setId(1L);
        a.setFileName("prog.nc");
        when(attachmentMapper.selectByDrawingId(1L)).thenReturn(List.of(a));
        Result<List<CrmDrawingAttachment>> r = service.listAttachments(1L);
        assertEquals(0, r.getCode());
        assertEquals(1, r.getData().size());
    }

    @Test
    void reject_unsupported_file_type() throws Exception {
        when(drawingMapper.selectById(1L)).thenReturn(new CrmDrawing());
        MockMultipartFile file = new MockMultipartFile("file", "virus.exe", "application/octet-stream", new byte[]{1});
        Result<CrmDrawingAttachment> r = service.uploadAttachment(1L, file, 1001L);
        assertEquals(40001, r.getCode());
        verify(fileStorage, never()).putObject(anyString(), any(java.io.InputStream.class), anyLong(), any());
    }
}
