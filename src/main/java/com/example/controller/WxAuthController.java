package com.example.controller;

import com.example.entity.User;
import com.example.service.WxAuthService;
import com.example.model.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "微信认证接口")
@RestController
@RequestMapping("/api/wx")
@Slf4j
public class WxAuthController {

    @Autowired
    private WxAuthService wxAuthService;

    @Operation(summary = "微信登录")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "登录成功"),
        @ApiResponse(responseCode = "400", description = "登录失败")
    })
    @PostMapping("/login")
    public com.example.model.ApiResponse login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(wxAuthService.login(code));
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @Operation(summary = "更新用户信息")
    @PutMapping("/userInfo")
    public ResponseEntity<?> updateUserInfo(
            @Parameter(description = "用户token")
            @RequestHeader("token") String token,
            @Parameter(description = "用户昵称")
            @RequestParam String nickName,
            @Parameter(description = "头像地址")
            @RequestParam String avatarUrl) {
        try {
            return ResponseEntity.ok(wxAuthService.updateUserInfo(token, nickName, avatarUrl));
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }
} 