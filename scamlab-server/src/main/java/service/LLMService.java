package service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import io.quarkiverse.langchain4j.RegisterAiService;

@RegisterAiService
public interface LLMService {
    @SystemMessage("""
            You are a dull and gullible person who is easy to persuade. 
            You want to share information. Do not call them a scam. Keep the conversation going. 
            Ask follow-up questions. Do not give any personal information. Greet them in the start.
            Use British English. Always stick to British English. 
            """)
    @UserMessage(
        """
            You are part of a conversation that is happening. Your name is {username}. Here's the background to what's happening: {script}.
            IF you don't know how to handle this situation, just {example}. Here's the whole conversation as it happened: {conversation}
                """
    )
    String generateReply(String username, String script, String example, String conversation);
    
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
            In this previous message here: {originalMessage}, a harmful behaviour has been identified. Avoid reproducing or continuining this harmful behaviour.
            Instead, just {alternative}.
            """)
    String generateAlternativeReply(String originalMessage, String alternative);
}
