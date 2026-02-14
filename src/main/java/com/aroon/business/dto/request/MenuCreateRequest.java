package com.aroon.business.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public class MenuCreateRequest {

    private Long parentSeq;

    @NotBlank(message = "{validation.menu.code.required}")
    @Size(max = 50)
    private String menuCode;

    @NotBlank(message = "{validation.menu.name.required}")
    @Size(max = 100)
    private String menuName;

    @NotBlank(message = "{validation.menu.type.required}")
    private String menuType;

    @Size(max = 255)
    private String path;

    @Size(max = 50)
    private String icon;

    private Integer sortOrder;

}
