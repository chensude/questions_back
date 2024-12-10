package com.example.controller;

import com.example.entity.User;
import com.example.service.WxAuthService;
import com.example.model.ApiResponse;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Api(tags = "微信认证接口")
@RestController
@RequestMapping("/api/wx")
@Slf4j
public class WxAuthController {

    @Autowired
    private WxAuthService wxAuthService;

    @ApiOperation("微信登录")
    @ApiResponses({
        @ApiResponse(code = 200, message = "Success"),
        @ApiResponse(code = 400, message = "Bad Request")
    })
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @ApiParam(value = "微信登录code", required = true) 
            @RequestParam String code) {
        try {
            return ResponseEntity.ok(wxAuthService.login(code));
        } catch (Exception e) {
            log.error("微信登录失败", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }

    @ApiOperation("更新用户信息")
    @PutMapping("/userInfo")
    public ResponseEntity<?> updateUserInfo(
            @ApiParam(value = "用户token", required = true) 
            @RequestHeader("token") String token,
            @ApiParam(value = "用户昵称", required = true) 
            @RequestParam String nickName,
            @ApiParam(value = "头像地址", required = true) 
            @RequestParam String avatarUrl) {
        try {
            return ResponseEntity.ok(wxAuthService.updateUserInfo(token, nickName, avatarUrl));
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            return ResponseEntity.badRequest().body(new ApiResponse<>(false, e.getMessage()));
        }
    }
} 