package com.example.survey.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.survey.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 按用户名查询用户（登录/注册校验）
    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User selectByUsername(String username);
}
