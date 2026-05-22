package com.ruc.platform.student.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class StudentImportBatchVO {

    private Integer totalCount = 0;

    private Integer successCount = 0;

    private Integer failureCount = 0;

    private List<StudentListItemVO> records = new ArrayList<>();

    private List<String> errors = new ArrayList<>();
}
