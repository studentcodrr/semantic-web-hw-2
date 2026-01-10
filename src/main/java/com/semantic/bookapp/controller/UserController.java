package com.semantic.bookapp.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

@Controller
public class UserController {


    @PostMapping("/select-user")
    public String selectUser(@RequestParam String userId, HttpSession session) {
        if (userId == null || userId.isEmpty()) {
            session.removeAttribute("currentUserId");
        } else {
            session.setAttribute("currentUserId", userId);
        }
        return "redirect:/"; 
    }
}