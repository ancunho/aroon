package com.aroon.business.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class MenuResponse {

    private Long menuSeq;
    private Long parentSeq;
    private String menuCode;
    private String menuName;
    private String menuType;
    private String path;
    private String icon;
    private Integer sortOrder;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private List<MenuResponse> children;

}
