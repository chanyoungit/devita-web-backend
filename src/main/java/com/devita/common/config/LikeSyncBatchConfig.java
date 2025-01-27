package com.devita.common.config;

import com.devita.common.exception.ResourceNotFoundException;
import com.devita.domain.post.domain.Post;
import com.devita.domain.post.dto.PostLikeDTO;
import com.devita.domain.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static com.devita.common.exception.ErrorCode.POST_NOT_FOUND;


@Configuration
@RequiredArgsConstructor
@Slf4j
public class LikeSyncBatchConfig {

    private final RedisTemplate<String, String> redisTemplate;
    private final PostRepository postRepository;
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    // 1. Job 설정
    @Bean
    public Job syncLikesJob(Step syncLikesStep) {
        return new JobBuilder("syncLikesJob", jobRepository)
                .start(syncLikesStep)
                .build();
    }

    @Bean
    public Step syncLikesStep() {
        return new StepBuilder("syncLikesStep", jobRepository)
                .<PostLikeDTO, Post>chunk(100, transactionManager)
                .reader(redisLikesReader())
                .processor(likesProcessor())
                .writer(dbLikesWriter())
                .build();
    }

    @Bean
    public ItemReader<PostLikeDTO> redisLikesReader() {
        return new ItemReader<>() {
//            private final Cursor<String> keys = redisTemplate.scan(ScanOptions.scanOptions()
//                    .match("post:like_count:*") // 패턴 지정
//                    .build());
            private final Set<String> keys = redisTemplate.keys("post:like_count:*");

            private final Iterator<String> keyIterator = keys.iterator();

            @Override
            public PostLikeDTO read() {
                if (!keyIterator.hasNext()) {
                    log.info("No more keys to read from Redis.");
                    return null; // 읽을 데이터가 없으면 null 반환
                }

                String countKey = keyIterator.next();
                String postId = countKey.replace("post:like_count:", "");

                Long likeCount = Long.parseLong(
                        redisTemplate.opsForValue().get(countKey)
                );

                log.info("postId:" + postId + " likeCount:" + likeCount);

                return new PostLikeDTO(Long.parseLong(postId), likeCount);
            }
        };
    }

    @Bean
    public ItemProcessor<PostLikeDTO, Post> likesProcessor() {
        return postLikeDTO -> {
            Post post = postRepository.findById(postLikeDTO.postId())
                    .orElseThrow(() -> new ResourceNotFoundException(POST_NOT_FOUND));

            log.info("Updating Post: " + post.getId() + " with LikeCount: " + postLikeDTO.likeCount());

            post.updateLikes(postLikeDTO.likeCount());
            return post;
        };
    }

    @Bean
    public ItemWriter<Post> dbLikesWriter() {
        return posts -> {
            log.info("Saving Posts: " + posts.size());
            postRepository.saveAll(posts);
        };
    }
}
