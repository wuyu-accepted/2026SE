package com.ruc.platform.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.notice.entity.NoticeFeedbackMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeFeedbackMessageMapper extends BaseMapper<NoticeFeedbackMessage> {

    @Select("SELECT * FROM notice_feedback_message WHERE feedback_id = #{feedbackId} ORDER BY created_at ASC, id ASC")
    List<NoticeFeedbackMessage> selectByFeedbackId(@Param("feedbackId") Long feedbackId);
}
