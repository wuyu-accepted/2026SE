package com.ruc.platform.notice.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class NoticeFeedbackDetailVO extends NoticeFeedbackVO {

    private List<NoticeFeedbackMessageVO> messages;
}
