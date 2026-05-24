package com.ruc.platform.honor.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("honor")
public class Honor {

    @TableId
    private Long id;

    private Long userId;

    private String title;

    private String studentName;

    private String studentNo;

    private String awardLevel;

    private String awardDate;

    private String term;

    private Long evidenceFileId;

    private String description;

    private String category;

    private Integer status;

    private Long createdBy;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
