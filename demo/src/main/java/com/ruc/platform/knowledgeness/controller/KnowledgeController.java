package com.ruc.platform.knowledgeness.controller;

import com.ruc.platform.common.api.PageResult;
import com.ruc.platform.common.api.Result;
import com.ruc.platform.knowledgeness.dto.KnowledgeArticleQueryDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeBehaviorDTO;
import com.ruc.platform.knowledgeness.dto.KnowledgeTemplateQueryDTO;
import com.ruc.platform.knowledgeness.service.KnowledgeService;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleDetailVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeArticleListItemVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeRecommendationVO;
import com.ruc.platform.knowledgeness.vo.KnowledgeTemplateVO;
import cn.dev33.satoken.stp.StpUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 知识控制器
 * 处理知识条目查询、模板下载等请求
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {

    private final KnowledgeService knowledgeService;

    /**
     * 获取知识条目列表
     * 支持关键词搜索、分类筛选
     * 
     * @param queryDTO 查询条件
     * @return 分页结果
     */
    @GetMapping("/articles")
    public Result<PageResult<KnowledgeArticleListItemVO>> listArticles(KnowledgeArticleQueryDTO queryDTO) {
        log.info("查询知识条目列表，条件: {}", queryDTO);
        // 默认查询已发布的条目
        if (queryDTO.getStatus() == null) {
            queryDTO.setStatus(1);
        }
        PageResult<KnowledgeArticleListItemVO> result = knowledgeService.listArticles(queryDTO);
        return Result.ok(result);
    }

    /**
     * 获取知识条目详情
     * 
     * @param id 条目ID
     * @return 条目详情
     */
    @GetMapping("/articles/{id}")
    public Result<KnowledgeArticleDetailVO> getArticleDetail(@PathVariable Long id) {
        log.info("获取知识条目详情，id: {}", id);
        KnowledgeArticleDetailVO detail = knowledgeService.getArticleDetail(id);
        return Result.ok(detail);
    }

    @GetMapping("/articles/{id}/source")
    public ResponseEntity<byte[]> downloadArticleSource(@PathVariable Long id) {
        KnowledgeArticleDetailVO detail = knowledgeService.getArticleDetail(id);
        String editorType = detail.getEditorType() == null || detail.getEditorType().isBlank() ? "markdown" : detail.getEditorType();
        String extension = "latex".equalsIgnoreCase(editorType) ? "tex" : "md";
        String filename = "knowledge-" + id + "." + extension;
        byte[] bytes = (detail.getSourceContent() == null ? "" : detail.getSourceContent()).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .contentLength(bytes.length)
                .body(bytes);
    }

    /**
     * 获取知识模板列表
     * 
     * @return 模板列表
     */
    @GetMapping("/templates")
    public Result<List<KnowledgeTemplateVO>> listTemplates(KnowledgeTemplateQueryDTO queryDTO) {
        log.info("获取知识模板列表");
        List<KnowledgeTemplateVO> templates = knowledgeService.listTemplates(queryDTO);
        return Result.ok(templates);
    }

    @GetMapping("/suggestions")
    public Result<List<String>> suggestions(@RequestParam String keyword, @RequestParam(required = false) Integer limit) {
        return Result.ok(knowledgeService.suggestKeywords(keyword, limit));
    }

    @GetMapping("/recommendations")
    public Result<List<KnowledgeRecommendationVO>> recommendations(@RequestParam(required = false) Integer limit) {
        Long userId = StpUtil.getLoginIdAsLong();
        return Result.ok(knowledgeService.listRecommendations(userId, limit));
    }

    @PostMapping("/behavior")
    public Result<Void> recordBehavior(@RequestBody KnowledgeBehaviorDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        knowledgeService.recordBehavior(userId, dto);
        return Result.ok();
    }
}
