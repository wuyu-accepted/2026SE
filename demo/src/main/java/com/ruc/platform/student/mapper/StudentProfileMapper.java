package com.ruc.platform.student.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.student.entity.StudentProfile;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import com.ruc.platform.student.vo.StudentListItemVO;

import java.util.List;

/**
 * 学生档案Mapper接口
 */
@Mapper
public interface StudentProfileMapper extends BaseMapper<StudentProfile> {

    /**
     * 根据用户ID查询学生档案
     * @param userId 用户ID
     * @return 学生档案实体
     */
    @Select("SELECT * FROM student_profile WHERE user_id = #{userId}")
    StudentProfile selectByUserId(@Param("userId") Long userId);

    /**
     * 根据学号查询学生档案
     * @param studentNo 学号
     * @return 学生档案实体
     */
    @Select("SELECT * FROM student_profile WHERE student_no = #{studentNo}")
    StudentProfile selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("""
            <script>
            SELECT
                sp.id,
                sp.user_id AS "userId",
                sp.student_no AS "studentNo",
                u.real_name AS "realName",
                u.phone,
                u.email,
                u.status,
                sp.gender,
                sp.grade,
                sp.major,
                sp.class_name AS "className",
                sp.political_status AS "politicalStatus",
                sp.auth_type AS "authType",
                sp.hometown,
                sp.dormitory,
                sp.updated_at AS "updatedAt"
            FROM student_profile sp
            INNER JOIN t_user u ON u.id = sp.user_id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(sp.student_no) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                        OR LOWER(u.real_name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                        OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                    )
                </if>
                <if test="grade != null and grade != ''">
                    AND sp.grade = #{grade}
                </if>
                <if test="major != null and major != ''">
                    AND sp.major = #{major}
                </if>
                <if test="className != null and className != ''">
                    AND sp.class_name = #{className}
                </if>
                <if test="authType != null and authType != ''">
                    AND sp.auth_type = #{authType}
                </if>
            </where>
            ORDER BY sp.updated_at DESC, sp.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<StudentListItemVO> selectStudentPage(@Param("keyword") String keyword,
                                              @Param("grade") String grade,
                                              @Param("major") String major,
                                              @Param("className") String className,
                                              @Param("authType") String authType,
                                              @Param("limit") Long limit,
                                              @Param("offset") Long offset);

    @Select("""
            <script>
            SELECT COUNT(1)
            FROM student_profile sp
            INNER JOIN t_user u ON u.id = sp.user_id
            <where>
                <if test="keyword != null and keyword != ''">
                    AND (
                        LOWER(sp.student_no) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                        OR LOWER(u.real_name) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                        OR LOWER(COALESCE(u.phone, '')) LIKE LOWER(CONCAT('%', #{keyword}, '%'))
                    )
                </if>
                <if test="grade != null and grade != ''">
                    AND sp.grade = #{grade}
                </if>
                <if test="major != null and major != ''">
                    AND sp.major = #{major}
                </if>
                <if test="className != null and className != ''">
                    AND sp.class_name = #{className}
                </if>
                <if test="authType != null and authType != ''">
                    AND sp.auth_type = #{authType}
                </if>
            </where>
            </script>
            """)
    Long countStudents(@Param("keyword") String keyword,
                       @Param("grade") String grade,
                       @Param("major") String major,
                       @Param("className") String className,
                       @Param("authType") String authType);
}
