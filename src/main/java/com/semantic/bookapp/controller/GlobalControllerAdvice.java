package com.semantic.bookapp.controller;

import com.semantic.bookapp.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import java.util.List;
import com.semantic.bookapp.model.User;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private RdfService rdfService;

    @ModelAttribute("users")
    public List<User> getUsers() {
        return rdfService.getAllUsers();
    }
}