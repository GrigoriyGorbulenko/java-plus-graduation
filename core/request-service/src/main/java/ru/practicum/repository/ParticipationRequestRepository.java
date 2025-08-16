package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import ru.practicum.model.ParticipationRequest;


import java.util.List;
import java.util.Optional;

public interface ParticipationRequestRepository extends JpaRepository<ParticipationRequest, Long> {

    Boolean existsByRequesterIdAndEventId(Long requesterId, Long event);

    Optional<ParticipationRequest> findByRequesterIdAndId(Long requesterId, Long requestId);

    List<ParticipationRequest> findAllByRequesterId(Long requesterId);

    List<ParticipationRequest> findAllByEventId(Long eventId);

    void deleteByRequesterId(Long requesterId);

    @Query("select p from ParticipationRequest p where p.status = 'CONFIRMED' and p.eventId in ?1")
    List<ParticipationRequest> findConfirmedRequests(List<Long> ids);
}
