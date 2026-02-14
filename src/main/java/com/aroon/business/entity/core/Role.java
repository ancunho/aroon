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
@TableName("role")
public class Role extends BaseEntity {

    @TableId(value = "role_seq", type = IdType.AUTO)
    private Long roleSeq;

    private String roleCode;

    private String roleName;

    private String description;

    private Integer status;


}
