package com.ruc.platform.honor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.honor.entity.Honor;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface HonorMapper extends BaseMapper<Honor> {

    @Select("""
            SELECT *
            FROM honor
            WHERE status = 1
              AND user_id = #{userId}
            ORDER BY
                term DESC,
                created_at DESC
            """)
    List<Honor> selectEnabledByUserIdOrderByTermDescCreatedAtDesc(@Param("userId") Long userId);
}
