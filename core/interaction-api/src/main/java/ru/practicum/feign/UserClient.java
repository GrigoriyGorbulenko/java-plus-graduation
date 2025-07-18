package ru.practicum.feign;

import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.user.NewUserRequest;
import ru.practicum.dto.user.UserDto;

import java.util.ArrayList;
import java.util.List;


@FeignClient(name = "user-service", path = "/admin/users")
public interface UserClient {

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "getAllUsersFallback")
    @GetMapping
    List<UserDto> getAllUsers(@RequestParam(defaultValue = "") List<Long> ids,
                              @PositiveOrZero @RequestParam(defaultValue = "0") Integer from,
                              @Positive @RequestParam(defaultValue = "10") Integer size) throws FeignException;

    @PostMapping
    UserDto saveUser(@RequestBody @Valid NewUserRequest newUserRequest) throws FeignException;

    @DeleteMapping("/{userId}")
    void deleteUser(@PathVariable Long userId) throws FeignException;

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "findByIdFallback")
    @GetMapping("/{userId}")
    UserDto findById(@PathVariable Long userId) throws FeignException;

    @CircuitBreaker(name = "defaultBreaker", fallbackMethod = "checkExistsByIdFallback")
    @GetMapping("/{userId}/exists")
    Boolean checkExistsById(@PathVariable Long userId) throws FeignException;

    @GetMapping("/{userId}")
    default UserDto findByIdFallback(Long userId, Throwable throwable) {
        return UserDto.builder()
                .id(userId)
                .name("UNKNOWN")
                .build();
    }

    @GetMapping
    default List<UserDto> getAllUsersFallback(List<Long> ids, Integer from, Integer size, Throwable throwable) {
        return new ArrayList<>();
    }

    @GetMapping("/{userId}/exists")
    default Boolean checkExistsByIdFallback(Long userId, Throwable throwable) {
        return false;
    }
}
