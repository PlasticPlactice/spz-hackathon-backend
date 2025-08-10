package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserSkillDto {
    private Long id;
    private String name;
    private String category;
    private Integer proficiency;
    private String level;
}