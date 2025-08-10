package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDto {
    private Long githubId;
    private String username;
    private String avatarUrl;
    private String department;
    private String jobTitle;
    private String selfIntroduction;
}
