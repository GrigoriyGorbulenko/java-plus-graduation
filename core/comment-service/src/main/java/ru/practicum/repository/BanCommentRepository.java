package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.practicum.model.BanComment;


import java.util.List;

public interface BanCommentRepository  extends JpaRepository<BanComment,Long> {
    boolean existsByEventIdAndUserId(Long eventId, Long userId);

    List<BanComment> findAllByUserId(Long userId);

    void deleteByEventIdAndUserId(Long eventId, Long userId);

    void deleteByUserId(Long userId);

}
