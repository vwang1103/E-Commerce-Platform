package com.vincentwang.ecommerce.common.messaging;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/internal/v1/events/dlt")
public final class KafkaDeadLetterReplayController {

    private final KafkaDeadLetterReplayService replayService;

    public KafkaDeadLetterReplayController(KafkaDeadLetterReplayService replayService) {
        this.replayService = replayService;
    }

    @PostMapping("/replay")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public DeadLetterReplayResult replay(@Valid @RequestBody ReplayRequest request) throws Exception {
        try {
            return replayService.replay(request.topic(), request.partition(), request.offset());
        } catch (KafkaDeadLetterReplayService.DeadLetterRecordNotFoundException exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, exception.getMessage(), exception);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }

    public record ReplayRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9._-]+\\.DLT") String topic,
            @Min(0) int partition,
            @Min(0) long offset) {}
}
