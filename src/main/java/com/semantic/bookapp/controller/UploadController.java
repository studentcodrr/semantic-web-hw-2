package com.semantic.bookapp.controller;

import com.semantic.bookapp.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class UploadController {
    
    @Autowired
    private RdfService rdfService;

    @GetMapping("/upload")
    public String uploadPage() {
        return "upload";
    }

    @PostMapping("/upload-rdf")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> uploadRdf(@RequestParam("rdfFile") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        
        if (file.isEmpty()) {
            response.put("success", false);
            response.put("error", "No file uploaded");
            return ResponseEntity.badRequest().body(response);
        }
        
        try {
            List<Map<String, String>> triples = rdfService.parseUploadedRdf(file.getInputStream());
            response.put("success", true);
            response.put("triples", triples);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
}