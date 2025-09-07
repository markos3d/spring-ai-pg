package dev.markos3d.spring_ai.rest.rag;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.markos3d.spring_ai.record.ChatRequest;
import dev.markos3d.spring_ai.rest.chat.ChatController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/rag-chat")
// @CrossOrigin(origins = "*") // Podesite prema potrebi
public class RagChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    private final OllamaChatModel ollamaChatModel;
    private final VectorStore vectorStore;

    public RagChatController(OllamaChatModel ollamaChatModel, VectorStore vectorStore) {
        this.ollamaChatModel = ollamaChatModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        
        if (request.message() == null || request.message().trim().isEmpty()) {
            return Flux.just("Please provide a valid message.");
        }

        try {
            log.info("Processing chat request: {}", request.message());
            
            // Optimize search request parameters
            SearchRequest searchRequest = SearchRequest.builder()
                .topK(5)  // more documents for better context
                .similarityThreshold(0.72) // slightly higher threshold
                .filterExpression("source == 'spring-boot-reference'") // filtrirati po izvoru
                .build();
                

            QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

            return ChatClient.builder(ollamaChatModel)
                .defaultSystem("""
                    You are an expert in Spring Boot and the Spring framework. 
                    Provide precise, technical answers with concrete code examples. 
                    Always cite the documentation on which you base your answer. 
                    Respond clearly and structured in the Serbian language.
                    """)
                .build()
                .prompt()
                .advisors(advisor)
                .user(request.message())
                .stream()
                .content()
                .doOnNext(token -> log.debug("Generated token: {}", token))
                .bufferUntil(token -> token.contains("\n") || token.contains("."))
                .map(tokens -> String.join("", tokens).trim())
                .filter(chunk -> !chunk.isEmpty())
                .onErrorResume(ex -> {
                    log.error("Error during chat processing", ex);
                    return Flux.just("Error processing your request. Please try again later.");
                });
                
        } catch (Exception e) {
            log.error("Unexpected error in chat controller", e);
            return Flux.just("Unexpected error occurred. Please try again later.");
        }
    }
    
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(status);
    }
}