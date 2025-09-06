package dev.markos3d.spring_ai.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;

@Configuration
@EnableConfigurationProperties
public class RagConfiguration {
    
    private static final Logger log = LoggerFactory.getLogger(RagConfiguration.class);
    
    @Bean
    @Primary
    public TextSplitter textSplitter() {
        return new TokenTextSplitter(
            800,    // chunk size
            100,    // min chunk size  
            10,     // min chunk length to embed
            0,      // max num chunks
            false   // keep separator
        );
    }
   
    
    @EventListener(ApplicationReadyEvent.class)
    public void logStartupInfo() {
        log.info("RAG application started successfully");
        log.info("Using Ollama models: chat=mistral, embedding=nomic-embed-text");
    }
}