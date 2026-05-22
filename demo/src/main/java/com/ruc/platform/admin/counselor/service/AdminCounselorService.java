package com.ruc.platform.admin.counselor.service;

import com.ruc.platform.admin.counselor.dto.CounselorCreateDTO;
import com.ruc.platform.admin.counselor.dto.CounselorUpdateDTO;
import com.ruc.platform.admin.counselor.vo.CounselorVO;

import java.util.List;

public interface AdminCounselorService {
    List<CounselorVO> listAll();
    void create(CounselorCreateDTO dto);
    void update(Long id, CounselorUpdateDTO dto);
    void approve(Long id);
    void delete(Long id);
}
