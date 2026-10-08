package com.gymflow.assistant.conversation;

import com.gymflow.assistant.shared.error.RateLimitException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AssistantRateLimiter {
    private final Map<String, ArrayDeque<Instant>> requests = new HashMap<>();
    private final int perMinute;
    private final Semaphore concurrency;

    public AssistantRateLimiter(@Value("${app.ai.requests-per-minute}") int perMinute,
                                @Value("${app.ai.max-concurrency}") int maxConcurrency) {
        this.perMinute = perMinute;
        this.concurrency = new Semaphore(maxConcurrency);
    }

    public <T> T execute(String subject, Supplier<T> action) {
        reserve(subject);
        if (!concurrency.tryAcquire()) throw new RateLimitException("O assistente está ocupado; tente novamente em instantes");
        try { return action.get(); }
        finally { concurrency.release(); }
    }

    private synchronized void reserve(String subject) {
        Instant threshold = Instant.now().minus(Duration.ofMinutes(1));
        ArrayDeque<Instant> times = requests.computeIfAbsent(subject, ignored -> new ArrayDeque<>());
        while (!times.isEmpty() && times.peekFirst().isBefore(threshold)) times.removeFirst();
        if (times.size() >= perMinute) throw new RateLimitException("Limite local do assistente atingido; aguarde um minuto");
        times.addLast(Instant.now());
    }
}
