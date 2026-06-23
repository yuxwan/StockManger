package com.luckyun.stock.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.luckyun.stock.entity.User;

import java.util.Map;

public interface UserService extends IService<User> {

    /** 登录，返回 token 和用户信息 */
    Map<String, Object> login(String username, String password);

    /** 修改密码 */
    void changePassword(long userId, String oldPassword, String newPassword);
}
