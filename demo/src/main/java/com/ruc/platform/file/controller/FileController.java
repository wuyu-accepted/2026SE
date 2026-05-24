package com.ruc.platform.file.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.common.api.ResultCode;
import com.ruc.platform.common.exception.BizException;
import com.ruc.platform.file.entity.FileMetadata;
import com.ruc.platform.file.service.FileService;
import com.ruc.platform.file.vo.FileUploadResultVO;
import com.ruc.platform.knowledgeness.service.KnowledgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileService fileService;
    private final KnowledgeService knowledgeService;

    private static final Set<String> PUBLIC_BIZ_TYPES = Set.of("template", "knowledge-template", "knowledge-file");

    @PostMapping("/upload")
    public Result<FileUploadResultVO> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "bizType", required = false, defaultValue = "attachment") String bizType) {
        long userId = StpUtil.getLoginIdAsLong();
        FileUploadResultVO result = fileService.uploadFile(file, bizType, userId);
        return Result.ok(result);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadFile(@PathVariable Long id) throws IOException {
        FileMetadata metadata = fileService.getFileMetadata(id);
        long userId = StpUtil.getLoginIdAsLong();

        if (!PUBLIC_BIZ_TYPES.contains(metadata.getBizType())) {
            if (!metadata.getUploaderId().equals(userId)) {
                throw new BizException(ResultCode.FORBIDDEN, "无权下载该文件");
            }
        }

        if (PUBLIC_BIZ_TYPES.contains(metadata.getBizType())) {
            knowledgeService.recordTemplateDownload(userId, id, "file-download");
        }

        try {
            byte[] fileBytes = Files.readAllBytes(Paths.get(metadata.getStoragePath()));
            String encodedFilename = URLEncoder.encode(metadata.getOriginName(), "UTF-8").replace("+", "%20");
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                    .contentType(MediaType.parseMediaType(metadata.getMimeType()))
                    .contentLength(metadata.getFileSize())
                    .body(fileBytes);
        } catch (IOException e) {
            log.error("文件读取失败，id: {}, path: {}", id, metadata.getStoragePath(), e);
            throw new BizException(ResultCode.NOT_FOUND, "文件不存在或已删除");
        }
    }
}
