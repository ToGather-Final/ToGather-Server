package com.example.user_service.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.user_service.support.BaseIntegrationTest;
import com.example.user_service.support.JsonSupport;
import com.example.user_service.support.TestFixtures;
import com.example.user_service.support.TokenSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

public class AuthFlowTest extends BaseIntegrationTest {

    @Test
    void 회원가입_로그인_보호API_리프레시_로그아웃() throws Exception {
        signup("a@test.com", "P@ssw0rd!", "태헌A");
        String loginJson = login("a@test.com", "P@ssw0rd!");
        String at = TokenSupport.accessToken(loginJson);
        me(at);
        String rt = TokenSupport.refreshToken(loginJson);
        String newAt = refresh(rt);
        me(newAt);
        logout(rt);
    }

    private void signup(String e, String p, String n) throws Exception {
        mockMvc.perform(
                        post("/auth/signup")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonSupport.toJson(TestFixtures.signupBody(e, p, n))))
                .andExpect(status().isCreated());
    }

    private String login(String e, String p) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(JsonSupport.toJson(TestFixtures.loginBody(e, p))))
                .andExpect(status().isOk())
                .andReturn();
        return result.getResponse().getContentAsString();
    }

    private void me(String at) throws Exception {
        mockMvc.perform(get("/users/me").header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").exists());
    }

    private String refresh(String rt) throws Exception {
        MvcResult result = mockMvc.perform(
                        post("/auth/refresh").header("X-Refresh-Token", rt)
                ).andExpect(status().isOk())
                .andReturn();
        return TokenSupport.accessToken(result.getResponse().getContentAsString());
    }

    private void logout(String rt) throws Exception {
        mockMvc.perform(post("/auth/logout").header("X-Refresh-Token", rt))
                .andExpect(status().isNoContent());
    }
}
