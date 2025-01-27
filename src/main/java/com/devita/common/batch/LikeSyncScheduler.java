package com.devita.common.batch;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class LikeSyncScheduler {

    private final JobLauncher jobLauncher;
    private final Job syncLikesJob;

    @Scheduled(cron = "0 0 0 * * ?")
    public void runLikeSyncJob() {
        log.info("Running like sync job...");
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(syncLikesJob, jobParameters);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
