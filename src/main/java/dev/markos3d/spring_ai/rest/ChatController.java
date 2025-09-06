package dev.markos3d.spring_ai.rest;

import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final OllamaChatModel ollamaChatModel;
    private final VectorStore vectorStore;

    public ChatController(OllamaChatModel ollamaChatModel, VectorStore vectorStore) {
        this.ollamaChatModel = ollamaChatModel;
        this.vectorStore = vectorStore;
    }

    @PostMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chat(@RequestBody String message) {

        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                .topK(3) // koliko dokumenata vraća
                .similarityThreshold(0.7) // filter po sličnosti
                .build())
                .build();

        return ChatClient.builder(ollamaChatModel)
                .build().prompt()
                .advisors(advisor)
                .user(message)
                .stream()
                .content()
                .bufferUntil(token -> token.contains("\n")) // skuplja dok ne naiđe newline
                .map(tokens -> String.join("", tokens).trim());
    }
}
