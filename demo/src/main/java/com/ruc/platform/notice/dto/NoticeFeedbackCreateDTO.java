package com.ruc.platform.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class NoticeFeedbackCreateDTO {

    @NotBlank(message = "反馈类型不能为空")
    private String feedbackType;

    @NotBlank(message = "反馈内容不能为空")
    @Size(max = 2000, message = "反馈内容不能超过2000字")
    private String content;
}
