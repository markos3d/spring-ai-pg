package dev.markos3d.spring_ai.rest.tools.action;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.markos3d.spring_ai.rest.tools.action.TaskManagementTools.TaskResult;

@RestController
public class TaskManagementController {

    private final ChatClient chatClient;
    private final TaskManagementTools taskManagementTools;

    public TaskManagementController(ChatClient.Builder builder, TaskManagementTools taskManagementTools) {
        this.chatClient = builder.build();
        this.taskManagementTools = taskManagementTools;
    }

    @GetMapping("/tasks")
    public TaskResult createTask(@RequestParam String message) {
        return chatClient.prompt()
                .tools(taskManagementTools)
                .user(message)
                .call()
                .entity(TaskResult.class);
    }
}
