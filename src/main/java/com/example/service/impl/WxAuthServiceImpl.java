package com.example.service.impl;

import org.springframework.stereotype.Service;
import com.example.service.WxAuthService;
import com.example.model.ApiResponse;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WxAuthServiceImpl implements WxAuthService {
    
    @Override
    public ApiResponse login(String code) {
        // TODO: 实现微信登录逻辑
        return new ApiResponse(true, "登录成功");
    }
    
    @Override
    public ApiResponse updateUserInfo(String token, String nickName, String avatarUrl) {
        // TODO: 实现更新用户信息逻辑
        return new ApiResponse(true, "更新成功");
    }
} 