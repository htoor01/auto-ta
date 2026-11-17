package io.github.htoor01.autota.grader;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
class GraderConfig {

    static final String EXECUTOR = "graderExecutor";

    /**
     * Exactly one worker, so submissions are graded one container at a time. The bounded queue turns
     * overload into a quick 503 instead of an ever-growing backlog. Queued work is not kept on shutdown:
     * those runs are marked FAILED on the next start.
     */
    @Bean(EXECUTOR)
    ThreadPoolTaskExecutor graderExecutor(GraderProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(properties.queueCapacity());
        executor.setThreadNamePrefix("grader-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        return executor;
    }
}
