package com.example.demo.Controller;

import com.example.demo.dto.AddSkillRequestDto;
import com.example.demo.dto.UserSkillDto;
import com.example.demo.entity.Skill;
import com.example.demo.entity.User;
import com.example.demo.entity.UserSkill;
import com.example.demo.repository.SkillRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.UserSkillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/me/skills")
public class SkillController {

    private final UserRepository userRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;

    public SkillController(UserRepository userRepository, SkillRepository skillRepository, UserSkillRepository userSkillRepository) {
        this.userRepository = userRepository;
        this.skillRepository = skillRepository;
        this.userSkillRepository = userSkillRepository;
    }

    // ログインユーザーのスキル一覧を取得
    @GetMapping
    public ResponseEntity<List<UserSkillDto>> getUserSkills() {
        User currentUser = getCurrentUser();
        List<UserSkill> userSkills = userSkillRepository.findByUserId(currentUser.getId());
        List<UserSkillDto> dtos = userSkills.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    // 新しいスキルを追加
    @PostMapping
    public ResponseEntity<UserSkillDto> addUserSkill(@RequestBody AddSkillRequestDto requestDto) {
        User currentUser = getCurrentUser();

        // skillsテーブルにスキルが存在しなければ新規作成
        Skill skill = skillRepository.findByName(requestDto.getName())
                .orElseGet(() -> {
                    Skill newSkill = new Skill();
                    newSkill.setName(requestDto.getName());
                    newSkill.setCategory(requestDto.getCategory());
                    return skillRepository.save(newSkill);
                });

        UserSkill newUserSkill = new UserSkill();
        newUserSkill.setUser(currentUser);
        newUserSkill.setSkill(skill);
        newUserSkill.setProficiency(requestDto.getProficiency());
        newUserSkill.setLevel(requestDto.getLevel());

        UserSkill savedUserSkill = userSkillRepository.save(newUserSkill);
        return ResponseEntity.status(HttpStatus.CREATED).body(convertToDto(savedUserSkill));
    }

    // 既存のスキルを更新
    @PutMapping("/{userSkillId}")
    public ResponseEntity<UserSkillDto> updateUserSkill(@PathVariable Long userSkillId, @RequestBody AddSkillRequestDto requestDto) {
        User currentUser = getCurrentUser();
        UserSkill userSkill = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UserSkill not found"));

        if (!userSkill.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only update your own skills");
        }

        userSkill.setProficiency(requestDto.getProficiency());
        userSkill.setLevel(requestDto.getLevel());
        UserSkill updatedUserSkill = userSkillRepository.save(userSkill);

        return ResponseEntity.ok(convertToDto(updatedUserSkill));
    }

    // スキルを削除
    @DeleteMapping("/{userSkillId}")
    public ResponseEntity<Void> deleteUserSkill(@PathVariable Long userSkillId) {
        User currentUser = getCurrentUser();
        UserSkill userSkill = userSkillRepository.findById(userSkillId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "UserSkill not found"));

        if (!userSkill.getUser().getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only delete your own skills");
        }

        userSkillRepository.delete(userSkill);
        return ResponseEntity.noContent().build();
    }

    // 認証情報から現在のユーザーを取得するヘルパーメソッド
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long githubId = Long.valueOf(authentication.getName());
        return userRepository.findByGithubId(githubId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    // EntityをDTOに変換するヘルパーメソッド
    private UserSkillDto convertToDto(UserSkill userSkill) {
        UserSkillDto dto = new UserSkillDto();
        dto.setId(userSkill.getId());
        dto.setName(userSkill.getSkill().getName());
        dto.setCategory(userSkill.getSkill().getCategory());
        dto.setProficiency(userSkill.getProficiency());
        dto.setLevel(userSkill.getLevel());
        return dto;
    }
}
