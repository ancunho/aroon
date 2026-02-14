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
@TableName("role_permission")
public class RolePermission extends BaseEntity {

    @TableId(value = "role_permission_seq", type = IdType.AUTO)
    private Long rolePermissionSeq;

    private Long roleSeq;

    private Long permissionSeq;

}
