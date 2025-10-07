package com.example.user_service.security;

import com.example.user_service.support.BaseIntegrationTest;
import com.example.user_service.support.JsonSupport;
import com.example.user_service.support.TestFixtures;
import com.example.user_service.support.TokenSupport;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class SecurityPreferenceTest extends BaseIntegrationTest {

    @Test
    void XUserId_헤더가_Bearer보다_우선() throws Exception {
        userAndToken("a@test.com");
    }

    private String userAndToken(String email) throws Exception {
        mockMvc.perform(post("/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(JsonSupport.toJson(TestFixtures.signupBody(email,"P@ssw0rd!","x"))))
                .andExpect(status().isCreated());
        String login = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonSupport.toJson(TestFixtures.loginBody(email, "P@ssw0rd!"))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + TokenSupport.accessToken(login);
    }

    private String meId(String bearer) throws Exception {
        MockHttpServletResponse result = mockMvc.perform(get("/users/me").header("Authorization", bearer))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        result.getContentAsString();
        return JsonPath.read(result,"$.id").toString();
    }
}
