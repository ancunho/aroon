package com.aroon.business.entity.core;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("role_menu")
public class RoleMenu extends BaseEntity {

    private Long roleSeq;

    private Long menuSeq;

}
