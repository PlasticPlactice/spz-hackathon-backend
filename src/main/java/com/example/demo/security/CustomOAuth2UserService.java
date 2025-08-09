package com.example.demo.security;

import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    public CustomOAuth2UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        Long githubId = Long.valueOf(attributes.get("id").toString());
        String username = (String) attributes.get("login");
        String avatarUrl = (String) attributes.get("avatar_url");
        String accessToken = userRequest.getAccessToken().getTokenValue();

        // DBにユーザーが存在するか確認し、存在しなければ新規登録、存在すれば情報を更新
        Optional<User> userOptional = userRepository.findByGithubId(githubId);
        User user;
        if (userOptional.isPresent()) {
            user = userOptional.get();
            user.setUsername(username);
            user.setAvatarUrl(avatarUrl);
            user.setGithubAccessToken(accessToken);
        } else {
            user = new User();
            user.setGithubId(githubId);
            user.setUsername(username);
            user.setAvatarUrl(avatarUrl);
            user.setGithubAccessToken(accessToken);
        }
        userRepository.save(user);

        return oauth2User;
    }
}