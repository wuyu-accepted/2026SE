package com.ruc.platform.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeFeedbackReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    @Size(max = 2000, message = "回复内容不能超过2000字")
    private String content;
}
