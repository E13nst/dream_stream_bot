package com.example.dream_stream_bot.test;

import com.example.dream_stream_bot.config.SecurityConfig;
import com.example.dream_stream_bot.controller.AdminWebController;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.security.TelegramAuthenticationFilter;
import com.example.dream_stream_bot.security.TelegramAuthenticationProvider;
import com.example.dream_stream_bot.service.admin.AdminUserDetailsService;
import com.example.dream_stream_bot.service.agent.AgentConfigService;
import com.example.dream_stream_bot.service.consent.ConsentService;
import com.example.dream_stream_bot.service.settings.SystemSettingsService;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {AdminWebController.class})
@Import(SecurityConfig.class)
class AdminSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private BotService botService;

    @MockBean
    private AgentConfigService agentConfigService;

    @MockBean
    private ConsentService consentService;

    @MockBean
    private TelegramAuthenticationFilter telegramAuthenticationFilter;

    @MockBean
    private TelegramAuthenticationProvider telegramAuthenticationProvider;

    @MockBean
    private AdminUserDetailsService adminUserDetailsService;

    @MockBean
    private SystemSettingsService systemSettingsService;

    @BeforeEach
    void setUp() {
        when(userService.findAll()).thenReturn(List.of());
        when(userService.findByRole(UserEntity.UserRole.ADMIN)).thenReturn(List.of());
        when(botService.findAll()).thenReturn(List.of());
        when(botService.findActiveBots()).thenReturn(List.of());
        when(systemSettingsService.getRetentionDaysAfterExpiry()).thenReturn(90);
        when(systemSettingsService.isRetentionUnlimited()).thenReturn(false);
        when(systemSettingsService.getInt(anyString(), anyInt())).thenAnswer(inv -> inv.getArgument(1));
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
    void logoutWithCsrfShouldSucceed() throws Exception {
        mockMvc.perform(post("/logout").with(csrf()))
                .andExpect(status().is3xxRedirection());
    }
}
