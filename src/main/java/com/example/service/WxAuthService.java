package com.example.service;

import com.example.model.ApiResponse;

public interface WxAuthService {
    ApiResponse login(String code);
    ApiResponse updateUserInfo(String token, String nickName, String avatarUrl);
} 