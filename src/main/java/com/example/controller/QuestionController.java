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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.List;

@RestController
@RequestMapping("/api/questions")
@Tag(name = "问题管理", description = "问题的增删改查接口")
@Slf4j
@RequiredArgsConstructor
public class QuestionController {
    
    @Autowired
    private WordParserService wordParserService;
    
    @Autowired
    private QuestionRepository questionRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Operation(summary = "上传Word文件")
    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(
            @Parameter(description = "Word文件", required = true) 
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "用户token", required = true) 
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
    
    @Operation(summary = "获取题目列表")
    @GetMapping
    public ResponseEntity<?> getQuestions(
            @Parameter(description = "题目类型") 
            @RequestParam(required = false) String questionType,
            @Parameter(description = "页码") 
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页数量") 
            @RequestParam(defaultValue = "10") int pageSize,
            @Parameter(description = "用户token", required = true) 
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