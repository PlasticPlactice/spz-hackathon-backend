package com.example.demo.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    @GetMapping("/login-success")
    public String loginSuccess() {
        return """
               <h1>Login Successful!</h1>
               <p>You can now close this page.</p>
               <p>The accessToken cookie has been set in your browser.</p>
               """;
    }
}