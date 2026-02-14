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
@TableName("role_menu")
public class RoleMenu extends BaseEntity {

    @TableId(value = "role_menu_seq", type = IdType.AUTO)
    private Long roleMenuSeq;

    private Long roleSeq;

    private Long menuSeq;

}
