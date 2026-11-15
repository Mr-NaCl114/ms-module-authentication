package com.lods.authentication.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.lods.authentication.domain.entity.AuthRole;
import org.apache.ibatis.annotations.Mapper;

/**
 * 角色表访问入口。CRUD 由 MyBatis-Plus 提供，复杂查询后续以 XML 或注解补充。
 */
@Mapper
public interface AuthRoleMapper extends BaseMapper<AuthRole> {
}
