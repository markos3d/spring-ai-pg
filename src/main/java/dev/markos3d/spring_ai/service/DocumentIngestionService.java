package dev.markos3d.spring_ai.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

@Service
public class DocumentIngestionService implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DocumentIngestionService.class);

    @Value("classpath:/pdf/spring-boot-reference.pdf")
    private Resource resource;

    private final VectorStore vectorStore;

    public DocumentIngestionService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    @Override
    public void run(String... args) {
        if (isDocumentAlreadyIngested()) {
            log.info("Document already ingested, skipping...");
            return;
        }

        try {
            log.info("Starting document ingestion...");

            TikaDocumentReader reader = new TikaDocumentReader(resource);

            TokenTextSplitter textSplitter = new TokenTextSplitter();

            List<Document> documents = reader.read();
            log.info("Read {} documents from PDF", documents.size());

            // Set richer metadata
            documents = documents.stream()
                    .map(this::enrichMetadata)
                    .collect(Collectors.toList());

            List<Document> chunks = textSplitter.split(documents);
            log.info("Split into {} chunks", chunks.size());

            // Batch ingestion for better performance
            vectorStore.accept(chunks);

            log.info("Successfully ingested {} chunks into vector store", chunks.size());

        } catch (Exception e) {
            log.error("Error during document ingestion", e);
            throw new RuntimeException("Failed to ingest documents", e);
        }
    }

    private Document enrichMetadata(Document doc) {
      // Add metadata directly to the existing document
        doc.getMetadata().put("source", "spring-boot-reference");
        doc.getMetadata().put("ingestion_date", LocalDateTime.now().toString());
        doc.getMetadata().put("document_type", "technical_documentation");
        doc.getMetadata().put("language", "en");

        // Zadrži postojeće metadata ako postoje
        if (doc.getMetadata().containsKey("page_number")) {
            doc.getMetadata().put("page", doc.getMetadata().get("page_number"));
        }

        // Return the same document with enriched metadata
        return doc;
    }

    private boolean isDocumentAlreadyIngested() {
        // Simple check - you can extend as needed
        try {
            SearchRequest searchRequest = SearchRequest.builder()
                    .topK(1)
                    // .similarityThreshold(0.9)
                    .filterExpression("source == 'spring-boot-reference'")
                    .build();

            List<Document> existing = vectorStore.similaritySearch(searchRequest);
            return !existing.isEmpty();
        } catch (Exception e) {
            log.warn("Could not check for existing documents", e);
            return false;
        }
    }
}