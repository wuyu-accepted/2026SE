package com.ruc.platform.studyanalysis.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ruc.platform.studyanalysis.entity.StudyCourseRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudyCourseRecordMapper extends BaseMapper<StudyCourseRecord> {

    @Select("SELECT * FROM study_course_record WHERE user_id = #{userId} ORDER BY updated_at DESC, id DESC")
    List<StudyCourseRecord> selectByUserId(@Param("userId") Long userId);
}
