package com.aroon.business.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class MenuUpdateRequest {

    private Long parentSeq;

    @Size(max = 100)
    private String menuName;

    private String menuType;

    @Size(max = 255)
    private String path;

    @Size(max = 50)
    private String icon;

    private Integer sortOrder;

    private Integer status;

}
