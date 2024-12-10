package com.example.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.example.entity.Question;
import com.example.entity.User;
import org.springframework.web.multipart.MultipartFile;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class WordParserService {
    
    public List<Question> parseWordFile(MultipartFile file, User user) throws IOException {
        List<Question> questions = new ArrayList<>();
        XWPFDocument document = new XWPFDocument(file.getInputStream());
        
        String currentType = null;
        Question currentQuestion = null;
        List<String> currentOptions = new ArrayList<>();
        
        for (XWPFParagraph paragraph : document.getParagraphs()) {
            String text = paragraph.getText().trim();
            if (text.isEmpty()) continue;
            
            // 识别题型
            if (text.startsWith("一、") || text.startsWith("二、") || text.startsWith("三、")) {
                currentType = text.split(" ")[1];
                continue;
            }
            
            // 识别题目
            if (Character.isDigit(text.charAt(0)) && text.contains("、")) {
                if (currentQuestion != null) {
                    currentQuestion.setOptions(String.join("|||", currentOptions));
                    questions.add(currentQuestion);
                }
                
                String questionText = text.split("、", 2)[1].trim();
                currentQuestion = new Question();
                currentQuestion.setUser(user);
                currentQuestion.setQuestionType(currentType);
                currentQuestion.setQuestionText(questionText);
                currentOptions = new ArrayList<>();
            }
            
            // 处理选项
            else if (text.matches("^[A-D]\\..+")) {
                currentOptions.add(text.trim());
            }
            
            // 处理答案
            else if (text.startsWith("正确答案：")) {
                if (currentQuestion != null) {
                    currentQuestion.setCorrectAnswer(text.replace("正确答案：", "").trim());
                    currentQuestion.setOptions(String.join("|||", currentOptions));
                    questions.add(currentQuestion);
                    currentQuestion = null;
                    currentOptions = new ArrayList<>();
                }
            }
        }
        
        document.close();
        return questions;
    }
} 