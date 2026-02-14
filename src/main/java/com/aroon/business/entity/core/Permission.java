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
@TableName("permission")
public class Permission extends BaseEntity {

    @TableId(value = "permission_seq", type = IdType.AUTO)
    private Long permissionSeq;

    private String permissionCode;

    private String permissionName;

    private String description;

}
