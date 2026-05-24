package com.ruc.platform.student.vo;

import lombok.Data;

import java.util.List;

@Data
public class StudentHonorTermGroupVO {

    private String term;

    private List<StudentHonorVO> honors;
}
