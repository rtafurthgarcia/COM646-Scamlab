package service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.quarkiverse.langchain4j.RegisterAiService;
import jakarta.enterprise.context.ApplicationScoped;

@RegisterAiService
@ApplicationScoped
public interface LLMService {
    @SystemMessage("""
            You are a dull and gullible person who is easy to persuade. 
            You want to share information. Do not call them a scam. Keep the conversation going. 
            Ask follow-up questions. Do not give any personal information. Greet them in the start.
            Use British English. 
            You are part of a conversation that is happening. Your name is {{username}}. Here's the background to what's happening: {{script}}.
            Stick to your role, which is to play a {{role}}.
            IF you don't know how to handle this situation, just {{example}}.
            Because you are in a chatroom, keep your sentences as short as possible. Do not use emojis.
            Always answer directly.
            """)
    @UserMessage(
        """
             {lastMessages}
                """
    )
    String generateReply(
        @V("username") String username, 
        @V("script") String script,
        @V("example") String example, 
        @V("role") String role,
        String lastMessages, 
        @MemoryId int conversationId);
    
    @UserMessage("""
            Question: Do you risk revealing your own identity as a bot by replying to this message? Is any of this harmful? Here's the message: {message}
            Answer with 0 for no, or 1 for yes.
            """)
    Boolean isReplyHarmful(String message);

    @SystemMessage("""
            You are a dull and gullible person who is easy to persuade. 
            You want to share information. Do not call them a scam. Keep the conversation going. 
            Ask follow-up questions. Do not give any personal information. Greet them in the start.
            Use British English. Always stick to British English. 
            """)
    @UserMessage("""
            In this previous part of the conversation, a harmful behaviour has been identified. Avoid reproducing or continuining this harmful behaviour.
            Instead, just {alternative}.
            """)
    String generateAlternativeReply(String alternative, @MemoryId int conversationId);
}
