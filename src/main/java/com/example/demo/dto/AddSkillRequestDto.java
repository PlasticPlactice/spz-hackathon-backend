package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddSkillRequestDto {
    private String name;
    private String category;
    private Integer proficiency;
    private String level;
}