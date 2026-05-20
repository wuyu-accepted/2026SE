package com.ruc.platform.party.vo;

import lombok.Data;

import java.util.List;

@Data
public class PartyGuidanceVO {

    private String title;

    private String content;

    private Integer priority;

    private List<String> materials;
}
