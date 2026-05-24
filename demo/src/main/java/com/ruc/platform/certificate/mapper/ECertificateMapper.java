package com.ruc.platform.certificate.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.certificate.entity.ECertificate;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ECertificateMapper extends BaseMapper<ECertificate> {

    @Select("""
            SELECT *
            FROM e_certificate
            WHERE user_id = #{userId}
            ORDER BY submit_time DESC
            """)
    List<ECertificate> selectByUserId(@Param("userId") Long userId);

    @Select("""
            SELECT COUNT(*)
            FROM e_certificate
            WHERE user_id = #{userId}
            """)
    Long countByUserId(@Param("userId") Long userId);
}
