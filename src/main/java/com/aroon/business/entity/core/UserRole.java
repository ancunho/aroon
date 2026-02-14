package com.aroon.business.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user_role")
public class UserRole extends BaseEntity {

    private Long userSeq;

    private Long roleSeq;

}
