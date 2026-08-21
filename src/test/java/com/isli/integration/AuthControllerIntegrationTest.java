package com.isli.integration;

import com.isli.dto.auth.LoginRequestDTO;
import com.isli.dto.auth.RegisterRequestDTO;
import com.isli.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registerThenLogin_thenAccessProtectedEndpointWithIssuedToken() throws Exception {
        RegisterRequestDTO register = RegisterRequestDTO.builder()
                .username("mvc-user").password("mvcpass123")
                .email("mvc-user@example.com").role(Role.ATTENDEE)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("mvc-user"))
                .andExpect(jsonPath("$.role").value("ATTENDEE"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        LoginRequestDTO login = LoginRequestDTO.builder().username("mvc-user").password("mvcpass123").build();

        String token = mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        String jwt = objectMapper.readTree(token).get("token").asText();

        mockMvc.perform(get("/api/bookings/me").header("Authorization", "Bearer " + jwt))
                .andExpect(status().isOk());
    }

    @Test
    void registerRejectsShortPasswordWithValidationError() throws Exception {
        RegisterRequestDTO badRequest = RegisterRequestDTO.builder()
                .username("shortpass").password("abc")
                .email("shortpass@example.com").role(Role.ATTENDEE)
                .build();

        mockMvc.perform(post("/api/auth/register")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.password").exists());
    }

    @Test
    void protectedEndpointRejectsRequestWithoutToken() throws Exception {
        mockMvc.perform(get("/api/bookings/me"))
                .andExpect(status().isForbidden());
    }
}
