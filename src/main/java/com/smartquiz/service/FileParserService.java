package com.smartquiz.service;

import com.smartquiz.entity.Question;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileParserService {

    List<Question> parseQuestionsFile(MultipartFile file);
}
