package com.aroon.business.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_permission")
public class RolePermission extends BaseEntity {

    private Long roleSeq;

    private Long permissionSeq;

}
