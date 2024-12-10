package com.example.controller;

import com.example.service.WxAuthService;
import com.example.model.ApiResponse;
import com.example.model.LoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "微信认证接口")
@RestController
@RequestMapping("/api/wx")
@Slf4j
@RequiredArgsConstructor
public class WxAuthController {

    private final WxAuthService wxAuthService;

    @Operation(summary = "微信登录")
    @PostMapping("/login")
    public ApiResponse login(@RequestBody LoginRequest request) {
        try {
            return wxAuthService.login(request.getCode());
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return new ApiResponse(false, e.getMessage());
        }
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/userInfo")
    public ApiResponse updateUserInfo(
            @Parameter(description = "用户token") @RequestHeader("token") String token,
            @Parameter(description = "用户昵称") @RequestParam String nickName,
            @Parameter(description = "头像地址") @RequestParam String avatarUrl) {
        try {
            return wxAuthService.updateUserInfo(token, nickName, avatarUrl);
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return new ApiResponse(false, e.getMessage());
        }
    }
} 