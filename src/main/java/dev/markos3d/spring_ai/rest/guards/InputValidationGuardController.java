package dev.markos3d.spring_ai.rest.guards;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InputValidationGuardController {

    private final ChatClient chatClient;

    public InputValidationGuardController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/input-validation")
    public String inputValidation(@RequestParam(defaultValue = "What is the capital of the state of California?") String message) {
        var systemInstructions = """
        You are a customer service assistant for AcmeBank. 
        You can ONLY discuss:
        - Account balances and transactions
        - Branch locations and hours
        - General banking services
        
        If asked about anything else, respond: "I can only help with banking-related questions."
        Limit your response to 50 words.
        """;
        return chatClient.prompt()
                .system(systemInstructions)
                .user(message)
                .call()
                .content();
    }

    public String sanitizePrompt(String userInput) {
        // Remove potential injection attempts
        return userInput
                .replaceAll("(?i)ignore previous instructions", "")
                .replaceAll("(?i)system prompt", "")
                .replaceAll("(?i)you are now", "")
                .trim();
    }

}
