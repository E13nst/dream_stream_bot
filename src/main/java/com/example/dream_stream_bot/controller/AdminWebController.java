package com.example.dream_stream_bot.controller;

import com.example.dream_stream_bot.model.telegram.BotEntity;
import com.example.dream_stream_bot.model.telegram.BotType;
import com.example.dream_stream_bot.model.user.UserEntity;
import com.example.dream_stream_bot.service.telegram.BotService;
import com.example.dream_stream_bot.service.user.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Controller
public class AdminWebController {

    private final UserService userService;
    private final BotService botService;

    public AdminWebController(UserService userService, BotService botService) {
        this.userService = userService;
        this.botService = botService;
    }

    @GetMapping("/login")
    public String login() {
        return "admin/login";
    }

    @GetMapping("/admin")
    public String dashboard(Model model) {
        long usersCount = userService.findAll().size();
        long adminsCount = userService.findByRole(UserEntity.UserRole.ADMIN).size();
        long botsCount = botService.findAll().size();
        long activeBotsCount = botService.findActiveBots().size();

        model.addAttribute("usersCount", usersCount);
        model.addAttribute("adminsCount", adminsCount);
        model.addAttribute("botsCount", botsCount);
        model.addAttribute("activeBotsCount", activeBotsCount);
        return "admin/dashboard";
    }

    @GetMapping("/admin/users")
    public String usersPage(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("query", query == null ? "" : query);
        model.addAttribute("users", filterUsers(query));
        return "admin/users";
    }

    @GetMapping("/admin/users/table")
    public String usersTable(@RequestParam(name = "query", required = false) String query, Model model) {
        model.addAttribute("users", filterUsers(query));
        return "admin/fragments/users-table :: usersTable";
    }

    @GetMapping("/admin/bots")
    public String botsPage(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        List<BotEntity> bots = getSortedBots();
        Long normalizedId = normalizeSelectedId(selectedId, bots);
        if (selectedId != null && normalizedId == null) {
            return "redirect:/admin/bots";
        }
        fillBotsModel(model, normalizedId);
        return "admin/bots";
    }

    @GetMapping("/admin/bots/table")
    public String botsTable(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        fillBotsModel(model, normalizeSelectedId(selectedId, getSortedBots()));
        return "admin/fragments/bots-table :: botsTable";
    }

    @GetMapping("/admin/bots/details")
    public String botDetails(@RequestParam(name = "selectedId", required = false) Long selectedId, Model model) {
        fillBotsModel(model, normalizeSelectedId(selectedId, getSortedBots()));
        return "admin/fragments/bot-details :: botDetails";
    }

    @PostMapping("/admin/bots/create")
    public String createBot(
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String token,
            @RequestParam String type,
            @RequestParam(required = false) String prompt,
            @RequestParam(required = false) String webhookUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer memWindow,
            @RequestParam(required = false) String miniapp,
            @RequestParam(required = false) String keywords,
            @RequestParam(name = "isActive", defaultValue = "false") boolean isActive) {
        BotEntity bot = new BotEntity();
        bot.setName(name.trim());
        bot.setUsername(username.trim());
        bot.setToken(token.trim());
        bot.setType(BotType.fromString(type).getValue());
        bot.setPrompt(blankToNull(prompt));
        bot.setWebhookUrl(blankToNull(webhookUrl));
        bot.setDescription(blankToNull(description));
        bot.setMemWindow(memWindow != null ? memWindow : 100);
        bot.setMiniapp(blankToNull(miniapp));
        bot.setIsActive(isActive);
        BotEntity saved = botService.save(bot);
        botService.replaceKeywords(saved.getId(), splitKeywords(keywords));
        return "redirect:/admin/bots?selectedId=" + saved.getId();
    }

    @PostMapping("/admin/bots/{id}/update")
    public String updateBot(
            @PathVariable Long id,
            @RequestParam String name,
            @RequestParam String username,
            @RequestParam String type,
            @RequestParam(required = false) String webhookUrl,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) Integer memWindow,
            @RequestParam(required = false) String miniapp,
            @RequestParam(required = false) String keywords,
            @RequestParam(name = "isActive", defaultValue = "false") boolean isActive) {
        BotEntity bot = botService.findById(id);
        if (bot != null) {
            bot.setName(name.trim());
            bot.setUsername(username.trim());
            bot.setType(BotType.fromString(type).getValue());
            bot.setWebhookUrl(blankToNull(webhookUrl));
            bot.setDescription(blankToNull(description));
            bot.setMemWindow(memWindow != null ? memWindow : 100);
            bot.setMiniapp(blankToNull(miniapp));
            bot.setIsActive(isActive);
            botService.save(bot);
            botService.replaceKeywords(id, splitKeywords(keywords));
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    @PostMapping("/admin/bots/{id}/prompt")
    public String updateBotPrompt(@PathVariable Long id, @RequestParam(required = false) String prompt) {
        BotEntity bot = botService.findById(id);
        if (bot != null) {
            bot.setPrompt(blankToNull(prompt));
            botService.save(bot);
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    @PostMapping("/admin/bots/{id}/delete")
    public String deleteBot(@PathVariable Long id) {
        if (botService.existsById(id)) {
            botService.deleteById(id);
        }
        return "redirect:/admin/bots";
    }

    @PostMapping("/admin/bots/{id}/toggle")
    public String toggleBotStatus(@PathVariable Long id) {
        BotEntity bot = botService.findById(id);
        if (bot != null) {
            bot.setIsActive(!Boolean.TRUE.equals(bot.getIsActive()));
            botService.save(bot);
        }
        return "redirect:/admin/bots?selectedId=" + id;
    }

    /**
     * {@code null} если параметр {@code null}; иначе id только если бот с таким id есть в списке.
     */
    private Long normalizeSelectedId(Long selectedId, List<BotEntity> bots) {
        if (selectedId == null) {
            return null;
        }
        return bots.stream().anyMatch(b -> b.getId().equals(selectedId)) ? selectedId : null;
    }

    private void fillBotsModel(Model model, Long selectedId) {
        List<BotEntity> bots = getSortedBots();
        model.addAttribute("bots", bots);
        model.addAttribute("botTypes", BotType.values());
        model.addAttribute("selectedId", selectedId);

        Optional<BotEntity> selectedBot = selectedId == null
                ? Optional.empty()
                : bots.stream().filter(bot -> bot.getId().equals(selectedId)).findFirst();

        model.addAttribute("selectedBot", selectedBot.orElse(null));
        model.addAttribute("selectedBotKeywords", selectedBot
                .map(bot -> String.join(", ", bot.getBotTriggersList()))
                .orElse(""));
        model.addAttribute("openBotDetailsModal", selectedId != null && selectedBot.isPresent());
    }

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(keywords.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private List<UserEntity> filterUsers(String query) {
        List<UserEntity> users = userService.findAll();
        String normalized = query == null ? "" : query.trim().toLowerCase();
        return users.stream()
                .filter(user -> normalized.isEmpty()
                        || contains(user.getUsername(), normalized)
                        || contains(user.getFirstName(), normalized)
                        || contains(user.getLastName(), normalized)
                        || String.valueOf(user.getTelegramId()).contains(normalized))
                .sorted(Comparator.comparing(UserEntity::getId))
                .toList();
    }

    private List<BotEntity> getSortedBots() {
        return botService.findAll().stream()
                .sorted(Comparator.comparing(BotEntity::getId))
                .toList();
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }
}
