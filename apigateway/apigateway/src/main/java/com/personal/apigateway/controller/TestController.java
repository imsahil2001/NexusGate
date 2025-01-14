package com.personal.apigateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class TestController {

    @GetMapping("/users")
    public String getUsers() {
        return "List of users";
    }

    @GetMapping("/products")
    public String getProducts() {
        return "List of products";
    }
}