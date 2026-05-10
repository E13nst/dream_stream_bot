package com.example.dream_stream_bot.service.ai;

import com.example.dream_stream_bot.model.agent.AgentConfigEntity;

public interface AIService {
    String completion(String conversationId, String userMessage, AgentConfigEntity agentConfig);
}
