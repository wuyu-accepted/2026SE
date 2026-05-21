package com.ruc.platform.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.notice.entity.NoticeFeedback;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface NoticeFeedbackMapper extends BaseMapper<NoticeFeedback> {

    @Select("SELECT * FROM notice_feedback WHERE student_user_id = #{studentUserId} AND notice_id = #{noticeId} ORDER BY created_at DESC")
    List<NoticeFeedback> selectByStudentAndNotice(@Param("studentUserId") Long studentUserId, @Param("noticeId") Long noticeId);

    @Select("SELECT * FROM notice_feedback WHERE status = 'pending_cadre' AND assigned_cadre_ids LIKE #{cadrePattern} ORDER BY updated_at DESC LIMIT #{limit} OFFSET #{offset}")
    List<NoticeFeedback> selectCadrePending(@Param("cadrePattern") String cadrePattern, @Param("limit") Long limit, @Param("offset") Long offset);

    @Select("SELECT COUNT(*) FROM notice_feedback WHERE status = 'pending_cadre' AND assigned_cadre_ids LIKE #{cadrePattern}")
    Long countCadrePending(@Param("cadrePattern") String cadrePattern);

    @Select("""
            <script>
            SELECT * FROM notice_feedback
            WHERE assigned_counselor_id = #{counselorUserId}
              AND status IN ('pending_counselor', 'pending_cadre')
              <if test="feedbackType != null and feedbackType != ''">
                AND feedback_type = #{feedbackType}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="noticeId != null">
                AND notice_id = #{noticeId}
              </if>
            ORDER BY updated_at DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<NoticeFeedback> selectCounselorPending(@Param("counselorUserId") Long counselorUserId,
                                                @Param("feedbackType") String feedbackType,
                                                @Param("status") String status,
                                                @Param("noticeId") Long noticeId,
                                                @Param("limit") Long limit,
                                                @Param("offset") Long offset);

    @Select("""
            <script>
            SELECT COUNT(*) FROM notice_feedback
            WHERE assigned_counselor_id = #{counselorUserId}
              AND status IN ('pending_counselor', 'pending_cadre')
              <if test="feedbackType != null and feedbackType != ''">
                AND feedback_type = #{feedbackType}
              </if>
              <if test="status != null and status != ''">
                AND status = #{status}
              </if>
              <if test="noticeId != null">
                AND notice_id = #{noticeId}
              </if>
            </script>
            """)
    Long countCounselorPendingFiltered(@Param("counselorUserId") Long counselorUserId,
                                       @Param("feedbackType") String feedbackType,
                                       @Param("status") String status,
                                       @Param("noticeId") Long noticeId);

    @Select("SELECT COUNT(*) FROM notice_feedback WHERE assigned_counselor_id = #{counselorUserId} AND status = 'pending_counselor'")
    Long countCounselorPending(@Param("counselorUserId") Long counselorUserId);
}
