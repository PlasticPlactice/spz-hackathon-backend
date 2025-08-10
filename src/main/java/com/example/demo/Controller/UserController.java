package com.example.demo.Controller;

import com.example.demo.dto.ProfileUpdateRequestDto;
import com.example.demo.dto.UserDto;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;

    public UserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        // JWTフィルターによってセットされた認証情報から、ユーザーのgithubIdを取得
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long githubId = Long.valueOf(authentication.getName());

        // githubIdを使ってデータベースからユーザー情報を検索
        return userRepository.findByGithubId(githubId)
                .map(this::convertToDto) // UserエンティティをUserDtoに変換
                .map(ResponseEntity::ok) // 成功した場合、200 OKと共にDTOを返す
                .orElse(ResponseEntity.notFound().build()); // ユーザーが見つからない場合、404 Not Foundを返す
    }

    @PutMapping("/me")
    public ResponseEntity<UserDto> updateCurrentUser(@RequestBody ProfileUpdateRequestDto updateRequest) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long githubId = Long.valueOf(authentication.getName());

        // データベースから現在のユーザー情報を取得
        return userRepository.findByGithubId(githubId)
                .map(user -> {
                    // DTOから受け取った情報でユーザーエンティティを更新
                    user.setDepartment(updateRequest.getDepartment());
                    user.setJobTitle(updateRequest.getJobTitle());
                    user.setSelfIntroduction(updateRequest.getSelfIntroduction());

                    // 更新したユーザー情報をデータベースに保存
                    User updatedUser = userRepository.save(user);

                    // 更新後の情報をDTOに変換して返す
                    return ResponseEntity.ok(convertToDto(updatedUser));
                })
                .orElse(ResponseEntity.notFound().build()); // ユーザーが見つからない場合は404
    }


    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setGithubId(user.getGithubId());
        dto.setUsername(user.getUsername());
        dto.setAvatarUrl(user.getAvatarUrl());
        dto.setDepartment(user.getDepartment());
        dto.setJobTitle(user.getJobTitle());
        dto.setSelfIntroduction(user.getSelfIntroduction());
        return dto;
    }
}
