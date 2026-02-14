package com.aroon.business.entity.core;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("menu")
public class Menu extends BaseEntity {

    @TableId(value = "menu_seq", type = IdType.AUTO)
    private Long menuSeq;

    private Long parentSeq;

    private String menuCode;

    private String menuName;

    private String menuType;

    private String path;

    private String icon;

    private Integer sortOrder;

    private Integer status;


}
