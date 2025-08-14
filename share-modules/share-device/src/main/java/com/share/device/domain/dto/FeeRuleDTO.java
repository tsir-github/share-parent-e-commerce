// FeeRuleDTO.java
package com.share.device.domain.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FeeRuleDTO {
    private Long id;
    private String name;
    private String rule;
    private String description;
    private String status;

    // 添加转换方法
    public static FeeRuleDTO fromMap(Map<String, Object> map) {
        FeeRuleDTO dto = new FeeRuleDTO();
        dto.setId(Long.parseLong(map.get("id").toString()));
        dto.setName((String) map.get("name"));
        dto.setRule((String) map.get("rule"));
        dto.setDescription((String) map.get("description"));
        dto.setStatus((String) map.get("status"));
        return dto;
    }
}