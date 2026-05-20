package com.ruc.platform.notice.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.notice.entity.UserMessage;
import com.ruc.platform.notice.vo.MessageDetailVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 用户消息Mapper
 */
@Mapper
public interface UserMessageMapper extends BaseMapper<UserMessage> {

    @Select("SELECT * FROM user_message WHERE user_id = #{userId} ORDER BY created_at DESC LIMIT #{limit}")
    List<UserMessage> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Select("SELECT COUNT(*) FROM user_message WHERE user_id = #{userId} AND read_status = 0")
    Long countUnreadByUserId(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM user_message WHERE notice_id = #{noticeId}")
    Long countByNoticeId(@Param("noticeId") Long noticeId);

    @Select("SELECT COUNT(*) FROM user_message WHERE notice_id = #{noticeId} AND read_status = 1")
    Long countReadByNoticeId(@Param("noticeId") Long noticeId);

    @Select("""
            SELECT
                um.id,
                um.notice_id AS "noticeId",
                um.title,
                um.summary,
                n.content,
                n.notice_type AS "noticeType",
                n.tag,
                n.priority,
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.id = #{id} AND um.user_id = #{userId}
            """)
    MessageDetailVO selectDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET read_status = 1, read_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND read_status = 0")
    int markAsReadByUserId(@Param("id") Long id, @Param("userId") Long userId);
}
