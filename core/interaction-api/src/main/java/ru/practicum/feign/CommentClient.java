package ru.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "comment-service", path = "/internal/comment")
public interface CommentClient {

    @DeleteMapping
    void deleteCommentsOfUser(@RequestParam Long userId) throws FeignException;
}
