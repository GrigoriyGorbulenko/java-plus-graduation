package ru.practicum.ewm.comment.model;

import jakarta.persistence.*;
import lombok.*;


@Entity
@Table(name = "ban_comments")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder
public class BanComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id")
    private Long userId;
    @Column(name = "event_id")
    private Long eventId;
}
