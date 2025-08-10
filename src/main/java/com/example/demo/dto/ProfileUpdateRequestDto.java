package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ProfileUpdateRequestDto {
    private String department;
    private String jobTitle;
    private String selfIntroduction;
}
