package ru.practicum.feign;

import feign.FeignException;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import ru.practicum.dto.category.CategoryDto;


@FeignClient(name = "category-service", path = "/categories")
public interface CategoryClient {

    @GetMapping("/{catId}")
    CategoryDto getCategoryById(@PathVariable Long catId) throws FeignException;
}
