package com.ruc.platform.admin.knowledge.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.ruc.platform.admin.knowledge.dto.KnowledgeArticleSaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeCategorySaveDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeStatusUpdateDTO;
import com.ruc.platform.admin.knowledge.dto.KnowledgeTemplateSaveDTO;
import com.ruc.platform.admin.knowledge.service.AdminKnowledgeService;
import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.entity.KnowledgeCategory;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/knowledge")
@RequiredArgsConstructor
public class AdminKnowledgeController {

    private final AdminKnowledgeService adminKnowledgeService;

    @GetMapping("/articles")
    public Result<PageResult<KnowledgeArticleListItemVO>> listArticles(KnowledgeArticleQueryDTO queryDTO) {
        return Result.ok(adminKnowledgeService.listArticles(queryDTO));
    }

    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticleDetailVO> getArticle(@PathVariable Long id) {
        return Result.ok(adminKnowledgeService.getArticle(id));
    }

    @PostMapping("/articles")
    public Result<Long> createArticle(@RequestBody KnowledgeArticleSaveDTO dto) {
        return Result.ok(adminKnowledgeService.createArticle(StpUtil.getLoginIdAsLong(), dto));
    }

    @PostMapping("/articles/preview")
    public Result<KnowledgeArticleDetailVO> previewArticle(@RequestBody KnowledgeArticleSaveDTO dto) {
        KnowledgeArticleDetailVO draft = new KnowledgeArticleDetailVO();
        draft.setEditorType(dto.getEditorType());
        draft.setSourceContent(dto.getSourceContent());
        return Result.ok(adminKnowledgeService.previewArticle(draft));
    }

    @GetMapping("/articles/{id}/source")
    public ResponseEntity<byte[]> downloadSource(@PathVariable Long id) {
        KnowledgeArticleDetailVO detail = adminKnowledgeService.getArticle(id);
        String editorType = detail.getEditorType() == null || detail.getEditorType().isBlank() ? "markdown" : detail.getEditorType();
        String extension = "latex".equals(editorType) ? "tex" : "md";
        String filename = "knowledge-" + id + "." + extension;
        byte[] bytes = (detail.getSourceContent() == null ? "" : detail.getSourceContent()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }

    @PutMapping("/articles/{id}")
    public Result<Void> updateArticle(@PathVariable Long id, @RequestBody KnowledgeArticleSaveDTO dto) {
        adminKnowledgeService.updateArticle(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }

    @PutMapping("/articles/{id}/status")
    public Result<Void> updateArticleStatus(@PathVariable Long id, @RequestBody KnowledgeStatusUpdateDTO dto) {
        adminKnowledgeService.updateArticleStatus(StpUtil.getLoginIdAsLong(), id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/articles/{id}")
    public Result<Void> deleteArticle(@PathVariable Long id) {
        adminKnowledgeService.deleteArticle(id);
        return Result.ok();
    }

    @GetMapping("/templates")
    public Result<List<KnowledgeTemplateVO>> listTemplates(KnowledgeTemplateQueryDTO queryDTO) {
        return Result.ok(adminKnowledgeService.listTemplates(queryDTO));
    }

    @PostMapping("/templates")
    public Result<Long> createTemplate(@RequestBody KnowledgeTemplateSaveDTO dto) {
        return Result.ok(adminKnowledgeService.createTemplate(StpUtil.getLoginIdAsLong(), dto));
    }

    @PutMapping("/templates/{id}")
    public Result<Void> updateTemplate(@PathVariable Long id, @RequestBody KnowledgeTemplateSaveDTO dto) {
        adminKnowledgeService.updateTemplate(StpUtil.getLoginIdAsLong(), id, dto);
        return Result.ok();
    }

    @PutMapping("/templates/{id}/status")
    public Result<Void> updateTemplateStatus(@PathVariable Long id, @RequestBody KnowledgeStatusUpdateDTO dto) {
        adminKnowledgeService.updateTemplateStatus(StpUtil.getLoginIdAsLong(), id, dto.getStatus());
        return Result.ok();
    }

    @DeleteMapping("/templates/{id}")
    public Result<Void> deleteTemplate(@PathVariable Long id) {
        adminKnowledgeService.deleteTemplate(id);
        return Result.ok();
    }

    @GetMapping("/categories")
    public Result<List<KnowledgeCategory>> listCategories() {
        return Result.ok(adminKnowledgeService.listCategories());
    }

    @PostMapping("/categories")
    public Result<Long> createCategory(@RequestBody KnowledgeCategorySaveDTO dto) {
        return Result.ok(adminKnowledgeService.createCategory(dto));
    }

    @PutMapping("/categories/{id}")
    public Result<Void> updateCategory(@PathVariable Long id, @RequestBody KnowledgeCategorySaveDTO dto) {
        adminKnowledgeService.updateCategory(id, dto);
        return Result.ok();
    }

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(adminKnowledgeService.stats());
    }
}
