package com.ruc.platform.admin.counselor.mapper;

import com.ruc.platform.admin.counselor.vo.CounselorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CounselorMapper {

    @Select("SELECT u.id, u.real_name AS realName, u.student_no AS studentNo, u.phone, u.status, u.created_at AS createdAt " +
            "FROM t_user u " +
            "INNER JOIN t_user_role ur ON ur.user_id = u.id " +
            "INNER JOIN t_role r ON r.id = ur.role_id " +
            "WHERE r.role_code = 'counselor' " +
            "ORDER BY u.status ASC, u.created_at DESC")
    List<CounselorVO> selectCounselors();
}
