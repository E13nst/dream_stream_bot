package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.config.SecurityConfig;
import com.example.dream_stream_bot.controller.AdminUserApiController;
import com.example.dream_stream_bot.controller.AdminWebController;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.security.TelegramAuthenticationFilter;
import com.example.dream_stream_bot.security.TelegramAuthenticationProvider;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminWebController.class, AdminUserApiController.class})
@Import(SecurityConfig.class)
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private BotService botService;

    @MockBean
    private TelegramAuthenticationFilter telegramAuthenticationFilter;

    @MockBean
    private TelegramAuthenticationProvider telegramAuthenticationProvider;

    @BeforeEach
    void setUp() {
        when(userService.findAll()).thenReturn(List.of());
        when(userService.findByRole(UserEntity.UserRole.ADMIN)).thenReturn(List.of());
        when(botService.findAll()).thenReturn(List.of());
        when(botService.findActiveBots()).thenReturn(List.of());

        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setTelegramId(100L);
        user.setUsername("user");
        user.setRole(UserEntity.UserRole.USER);
        user.setArtBalance(10L);
        when(userService.updateArtBalance(anyLong(), anyLong())).thenReturn(user);
    }

    @Test
    void guestShouldBeRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void userRoleShouldBeForbiddenForAdminPage() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminShouldAccessAdminPage() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminPatchWithCsrfShouldWork() throws Exception {
        mockMvc.perform(patch("/api/admin/users/1/balance")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("100"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void logoutWithCsrfShouldSucceed() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
