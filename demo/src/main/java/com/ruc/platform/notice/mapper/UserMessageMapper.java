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

    @Select("""
            SELECT
                um.*,
                n.attachment_file_id AS attachment_file_id
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId} AND n.status = 1
            ORDER BY um.pinned_status DESC, um.pinned_time DESC NULLS LAST, um.created_at DESC
            LIMIT #{limit}
            """)
    List<UserMessage> selectRecentByUserId(@Param("userId") Long userId, @Param("limit") Integer limit);


    @Select("""
            SELECT
                um.*,
                n.attachment_file_id AS attachment_file_id
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId} AND n.status = 1
            ORDER BY um.created_at DESC
            """)
    List<UserMessage> selectAllByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId} AND um.read_status = 0 AND n.status = 1
            """)
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
                n.attachment_file_id AS "attachmentFileId",
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                um.pinned_status AS "pinnedStatus",
                um.pinned_time AS "pinnedTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId}
              AND (um.title ILIKE CONCAT('%', #{keyword}, '%')
                OR um.summary ILIKE CONCAT('%', #{keyword}, '%')
                OR n.title ILIKE CONCAT('%', #{keyword}, '%')
                OR n.summary ILIKE CONCAT('%', #{keyword}, '%')
                OR n.tag ILIKE CONCAT('%', #{keyword}, '%')
                OR n.content ILIKE CONCAT('%', #{keyword}, '%'))
            ORDER BY um.pinned_status DESC, um.pinned_time DESC NULLS LAST, um.created_at DESC
            LIMIT #{limit}
            """)
    List<MessageDetailVO> searchMessagesByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") Integer limit);

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
                n.attachment_file_id AS "attachmentFileId",
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                um.pinned_status AS "pinnedStatus",
                um.pinned_time AS "pinnedTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.id = #{id} AND um.user_id = #{userId}
            """)
    MessageDetailVO selectDetailByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);

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
                n.attachment_file_id AS "attachmentFileId",
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                um.pinned_status AS "pinnedStatus",
                um.pinned_time AS "pinnedTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.notice_id = #{noticeId} AND um.user_id = #{userId}
            ORDER BY um.created_at DESC
            LIMIT 1
            """)
    MessageDetailVO selectDetailByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

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
                n.attachment_file_id AS "attachmentFileId",
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                um.pinned_status AS "pinnedStatus",
                um.pinned_time AS "pinnedTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId}
              AND (LOWER(COALESCE(um.title, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                OR LOWER(COALESCE(um.summary, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                OR LOWER(COALESCE(n.title, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                OR LOWER(COALESCE(n.summary, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                OR LOWER(COALESCE(n.tag, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                OR LOWER(COALESCE(n.content, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%')) )
            ORDER BY um.pinned_status DESC, um.pinned_time DESC NULLS LAST, um.created_at DESC
            LIMIT #{limit}
            """)
    List<MessageDetailVO> searchByKeyword(@Param("userId") Long userId, @Param("keyword") String keyword, @Param("limit") Integer limit);

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
                n.attachment_file_id AS "attachmentFileId",
                um.read_status AS "readStatus",
                um.read_time AS "readTime",
                um.pinned_status AS "pinnedStatus",
                um.pinned_time AS "pinnedTime",
                n.publish_time AS "publishTime",
                um.created_at AS "createdAt"
            FROM user_message um
            INNER JOIN notice n ON n.id = um.notice_id
            WHERE um.user_id = #{userId}
              AND n.status = 1
              AND LOWER(COALESCE(n.content, '')) LIKE '%mp.weixin.qq.com%'
            ORDER BY um.pinned_status DESC, um.pinned_time DESC NULLS LAST, um.created_at DESC
            LIMIT #{limit}
            """)
    List<MessageDetailVO> selectVisibleWechatArticleMessages(@Param("userId") Long userId, @Param("limit") Integer limit);

    @Update("UPDATE user_message SET read_status = 1, read_time = NOW() WHERE id = #{id} AND user_id = #{userId} AND read_status = 0")
    int markAsReadByUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET pinned_status = 1, pinned_time = NOW() WHERE id = #{id} AND user_id = #{userId}")
    int pinByUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET pinned_status = 1, pinned_time = NOW() WHERE notice_id = #{noticeId} AND user_id = #{userId}")
    int pinByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);

    @Update("UPDATE user_message SET pinned_status = 0, pinned_time = NULL WHERE id = #{id} AND user_id = #{userId}")
    int unpinByUserId(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE user_message SET pinned_status = 0, pinned_time = NULL WHERE notice_id = #{noticeId} AND user_id = #{userId}")
    int unpinByNoticeIdAndUserId(@Param("noticeId") Long noticeId, @Param("userId") Long userId);
}
