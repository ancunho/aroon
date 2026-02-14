package com.aroon.business.entity.core;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;


@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("user")
public class User extends BaseEntity {

    @TableId(value = "user_seq", type = IdType.AUTO)
    private Long userSeq;

    private String userId;

    private String password;

    private String email;

    private String nickname;

    private String loginLockYn;

    private String mobile;

    private Integer status;

    private LocalDateTime lastLoginDate;



}
