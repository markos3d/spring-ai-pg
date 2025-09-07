package dev.markos3d.spring_ai.rest.multimodal.image;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
public class ImageDetection {

    // MORA SE PROMENITI MODEL NA GEMMA3:4B-IT-QAT JER MISTRAL NE PODRŽAVA SLIKE

    private final ChatClient chatClient;
    @Value("classpath:/images/sincerely-media-2UlZpdNzn2w-unsplash.jpg")
    Resource sampleImage;

    public ImageDetection(ChatClient.Builder builder) {
        this.chatClient = builder
                .defaultOptions(ChatOptions.builder().model("gemma3:4b-it-qat").build())
                .build();
    }

    @GetMapping("/image-to-text")
    public String image() throws IOException {
        return chatClient.prompt()
                .user(u -> u
                        .text("Can you please explain what you see in the following image?")
                        .media(MimeTypeUtils.IMAGE_JPEG, sampleImage)
                )
                .call()
                .content();
    }
}

