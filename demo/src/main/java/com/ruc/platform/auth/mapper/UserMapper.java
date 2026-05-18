package com.ruc.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.auth.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户Mapper接口
 * 继承MyBatis-Plus的BaseMapper，获得基础CRUD能力
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 根据学号查询用户
     * @param studentNo 学号
     * @return 用户实体
     */
    @Select("SELECT * FROM t_user WHERE student_no = #{studentNo}")
    User selectByStudentNo(@Param("studentNo") String studentNo);

    @Select("SELECT * FROM t_user WHERE real_name = #{realName}")
    List<User> selectByRealName(@Param("realName") String realName);

    /**
     * 根据用户ID查询角色列表
     * @param userId 用户ID
     * @return 角色编码列表
     */
    @Select("SELECT r.role_code FROM t_role r " +
            "INNER JOIN t_user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId}")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT * FROM t_user WHERE real_name = #{realName}")
    List<User> selectByRealName(@Param("realName") String realName);
}
