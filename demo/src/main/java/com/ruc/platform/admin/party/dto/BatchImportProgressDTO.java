package com.ruc.platform.admin.party.dto;

import lombok.Data;

import java.util.List;

@Data
public class BatchImportProgressDTO {
    private List<Item> items;

    @Data
    public static class Item {
        private Long userId;
        private String stageCode;
        private String stepCode;
    }
}
