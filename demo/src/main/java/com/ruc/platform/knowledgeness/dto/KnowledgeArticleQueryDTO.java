package com.ruc.platform.knowledgeness.dto;

import lombok.Data;

/**
 * 知识条目查询DTO
 */
@Data
public class KnowledgeArticleQueryDTO {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 分类ID
     */
    private Long categoryId;

    private String contentType;

    private String tag;

    private String scenarioCode;

    /**
     * 状态：0-草稿，1-已发布，2-已下架
     */
    private Integer status;

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页大小
     */
    private Integer pageSize = 10;
}
