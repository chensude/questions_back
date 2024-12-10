package com.example.controller;

import com.example.entity.User;
import com.example.entity.Question;
import com.example.repository.UserRepository;
import com.example.repository.QuestionRepository;
import com.example.service.WordParserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.annotations.*;

import java.util.HashMap;
import java.util.List;

@Api(tags = "题库管理接口")
@RestController
@RequestMapping("/api")
@Slf4j
public class QuestionController {
    
    @Autowired
    private WordParserService wordParserService;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @ApiOperation("上传Word文件")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @ApiParam(value = "Word文件", required = true) 
            @RequestParam("file") MultipartFile file,
            @ApiParam(value = "用户token", required = true) 
            @RequestHeader("token") String token) {
        try {
            User user = userRepository.findByOpenid(token)
                    .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            List<Question> questions = wordParserService.parseWordFile(file, user);
            List<Question> savedQuestions = questionRepository.saveAll(questions);
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("status", "success");
                put("message", String.format("成功解析并导入 %d 个题目", savedQuestions.size()));
                put("data", new HashMap<String, Object>() {{
                    put("total_questions", savedQuestions.size());
                }});
            }});
            
        } catch (Exception e) {
            log.error("文件处理失败", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }
    
    @ApiOperation("获取题目列表")
    @GetMapping("/questions")
    public ResponseEntity<?> getQuestions(
            @ApiParam("题目类型") 
            @RequestParam(required = false) String questionType,
            @ApiParam("页码") 
            @RequestParam(defaultValue = "1") int page,
            @ApiParam("每页数量") 
            @RequestParam(defaultValue = "10") int pageSize,
            @ApiParam(value = "用户token", required = true) 
            @RequestHeader("token") String token) {
        try {
            User user = userRepository.findByOpenid(token)
                    .orElseThrow(() -> new RuntimeException("用户未找到"));
            
            Pageable pageable = PageRequest.of(page - 1, pageSize, Sort.by("id").descending());
            Page<Question> questionPage;
            
            if (questionType != null) {
                questionPage = questionRepository.findByUserAndQuestionType(user, questionType, pageable);
            } else {
                questionPage = questionRepository.findByUser(user, pageable);
            }
            
            return ResponseEntity.ok(new HashMap<String, Object>() {{
                put("status", "success");
                put("data", new HashMap<String, Object>() {{
                    put("total", questionPage.getTotalElements());
                    put("page", page);
                    put("page_size", pageSize);
                    put("questions", questionPage.getContent());
                }});
            }});
            
        } catch (Exception e) {
            log.error("查询失败", e);
            return ResponseEntity.badRequest().body(new HashMap<String, Object>() {{
                put("status", "error");
                put("message", e.getMessage());
            }});
        }
    }
} 