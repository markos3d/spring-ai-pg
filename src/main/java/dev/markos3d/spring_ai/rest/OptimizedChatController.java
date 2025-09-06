package dev.markos3d.spring_ai.rest;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.markos3d.spring_ai.record.ChatRequest;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat-optimized")
@CrossOrigin(origins = "*") // Podesite prema potrebi
public class OptimizedChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    private final OllamaChatModel ollamaChatModel;
    private final VectorStore vectorStore;

    public OptimizedChatController(OllamaChatModel ollamaChatModel, VectorStore vectorStore) {
        this.ollamaChatModel = ollamaChatModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody ChatRequest request) {
        
        if (request.message() == null || request.message().trim().isEmpty()) {
            return Flux.just("Molimo unesite validno pitanje.");
        }

        try {
            log.info("Processing chat request: {}", request.message());
            
            // Napredni search sa optimizovanim parametrima
            SearchRequest searchRequest = SearchRequest.builder()
                .topK(5)  // povećano sa 3 na 5 za bolji kontekst
                .similarityThreshold(0.72) // blago sniženo za više rezultata
                .filterExpression("source == 'spring-boot-reference'") // filtriranje po source
                .build();
                

            QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(searchRequest)
                .build();

            return ChatClient.builder(ollamaChatModel)
                .defaultSystem("""
                    Ti si stručnjak za Spring Boot i Spring framework.
                    Daj precizne, tehničke odgovore sa konkretnim primerima koda.
                    Uvek citiraj deo dokumentacije na kome zasnivas odgovor.
                    Odgovori jasno i strukturirano na srpskom jeziku.
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
                    return Flux.just("Došlo je do greške prilikom obrade zahteva. Molimo pokušajte ponovo.");
                });
                
        } catch (Exception e) {
            log.error("Unexpected error in chat controller", e);
            return Flux.just("Neočekivana greška. Molimo kontaktirajte podršku.");
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