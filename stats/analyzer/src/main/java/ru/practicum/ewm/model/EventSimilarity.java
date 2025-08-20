package ru.practicum.ewm.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.Instant;

@Entity
@Table(name = "events_similarity")
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EventSimilarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "event_a")
    Long eventA;

    @Column(name = "event_b")
    Long eventB;

    Double score;

    Instant timestamp;
}
