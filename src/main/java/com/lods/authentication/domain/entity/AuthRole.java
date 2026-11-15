package com.lods.authentication.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 业务角色。继承关系由 roleLevel 升序聚合计算，不在库内展开。
 */
@Data
@TableName("ms_auth_role")
public class AuthRole {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String roleCode;

    private String roleName;

    /** 权限等级，越大权限越高 */
    private Integer roleLevel;

    private String parentRoleCode;

    private Integer builtIn;

    private String description;

    private Integer sortOrder;

    private String status;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private Integer version;

    @TableLogic
    private Integer deleted;
}
