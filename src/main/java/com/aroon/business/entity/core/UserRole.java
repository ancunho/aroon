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
@TableName("user_role")
public class UserRole extends BaseEntity {

    @TableId(value = "user_role_seq", type = IdType.AUTO)
    private Long userRoleSeq;

    private Long userSeq;

    private Long roleSeq;

}
