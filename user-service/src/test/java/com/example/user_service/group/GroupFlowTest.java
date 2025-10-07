package com.example.user_service.group;

import com.example.user_service.support.BaseIntegrationTest;
import com.example.user_service.support.JsonSupport;
import com.example.user_service.support.TestFixtures;
import com.example.user_service.support.TokenSupport;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class GroupFlowTest extends BaseIntegrationTest {

    @Test
    void 그룹생성_초대_수락_멤버조회() throws Exception {
        String a = TokenSupport.accessToken(loginFlow("a@test.com"));
        String b = TokenSupport.accessToken(loginFlow("b@test.com"));
        long groupId = createGroup(a);
        String code = issueInvite(a, groupId);
        acceptInvite(b, code);
        listMembers(a, groupId);
    }

    private String loginFlow(String email) throws Exception {
        signup(email, "P@ssw0rd!", email.split("@")[0]);
        String login = login(email, "P@ssw0rd!");
        return login;
    }

    private void signup(String e, String p, String n) throws Exception{
        mockMvc.perform(post("/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonSupport.toJson(TestFixtures.signupBody(e, p, n))))
                .andExpect(status().isCreated());
    }

    private String login(String e, String p) throws Exception {
        return mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JsonSupport.toJson(TestFixtures.loginBody(e, p))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long createGroup(String at) throws Exception {
        String body = """
                { "name": "여행가자", "voteRule": "MAJORITY" }
                """;
        MvcResult result = mockMvc.perform(post("/groups")
                        .header("Authorization", "Bearer " + at)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    private String issueInvite(String at, long gid) throws Exception {
        MvcResult result = mockMvc.perform(post("/groups/{id}/invites", gid)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isCreated())
                .andReturn();
        return result.getResponse().getContentAsString().replace("\"", "");
    }

    private void acceptInvite(String at, String code) throws Exception {
        mockMvc.perform(post("/groups/invites/{code}/accept", code)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isNoContent());
    }

    private void listMembers(String at, long gid) throws Exception {
        mockMvc.perform(get("/groups/{id}/members", gid)
                        .header("Authorization", "Bearer " + at))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").exists());
    }
}
