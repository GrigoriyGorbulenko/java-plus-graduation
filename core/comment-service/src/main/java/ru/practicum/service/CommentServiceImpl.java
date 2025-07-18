package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.dto.comment.CommentDto;
import ru.practicum.dto.comment.NewCommentDto;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.dto.user.UserDtoForAdmin;
import ru.practicum.enums.comment.SortType;
import ru.practicum.enums.event.State;
import ru.practicum.exception.NotFoundException;
import ru.practicum.exception.ValidationException;
import ru.practicum.feign.EventClient;
import ru.practicum.feign.UserClient;
import ru.practicum.mapper.CommentMapper;
import ru.practicum.model.BanComment;
import ru.practicum.model.Comment;
import ru.practicum.repository.BanCommentRepository;
import ru.practicum.repository.CommentRepository;


import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {
    private final BanCommentRepository banRepository;
    private final CommentRepository commentRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Transactional
    @Override
    public CommentDto createComment(Long eventId, Long userId, NewCommentDto newCommentDto) {
        checkEventId(eventId);
        EventFullDto event = checkEvent(eventId);
        if (event.getState() != State.PUBLISHED) {
            throw new ValidationException("Нельзя комментировать не опубликованное событие");
        }
        UserDto user = checkUser(userId);
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        if (banRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ValidationException("Для данного пользователя стоит запрет на комментирование данного события");
        }
        if (!event.getCommenting()) {
            throw new ValidationException("Данное событие нельзя комментировать");
        }
        Comment comment = CommentMapper.toComment(newCommentDto, eventId, userId);
        return CommentMapper.toCommentDto(commentRepository.save(comment), event.getAnnotation(), user.getName());
    }

    @Transactional
    @Override
    public CommentDto updateComment(Long userId, Long eventId, Long commentId, NewCommentDto newCommentDto) {
        checkEventId(eventId);
        UserDto user = checkUser(userId);
        EventFullDto event = checkEvent(eventId);

        Comment comment = checkComment(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        if (comment.getAuthorId().equals(userId)) {
            comment.setText(newCommentDto.getText());
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
        return CommentMapper.toCommentDto(comment, event.getAnnotation(), user.getName());
    }

    @Transactional
    @Override
    public void deleteComment(Long userId, Long eventId, Long commentId) {
        checkEventId(eventId);
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        if (!eventClient.checkExistsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }
        Comment comment = checkComment(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        if (comment.getAuthorId().equals(userId)) {
            commentRepository.deleteById(commentId);
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
    }

    @Transactional
    @Override
    public void deleteComment(Long commentId, Long eventId) {
        checkEventId(eventId);
        Comment comment = checkComment(commentId);
        if (!Objects.equals(comment.getEventId(), eventId)) {
            throw new ValidationException("Некорректно указан eventId");
        }
        commentRepository.deleteById(commentId);
    }

    @Override
    public List<CommentDto> getAllComments(Long eventId, SortType sortType, Integer from, Integer size) {
        Pageable pageable = PageRequest.of(from / size, size);
        String eventName = checkEvent(eventId).getAnnotation();
        List<Comment> comments = commentRepository.findAllByEventId(eventId, pageable);
        List<Long> userIds = comments.stream()
                .map(Comment::getAuthorId)
                .distinct()
                .toList();
        Map<Long, String> userNames = userClient.getAllUsers(userIds, 0, userIds.size()).stream()
                .collect(Collectors.toMap(UserDto::getId, UserDto::getName));

        List<CommentDto> commentsResult = comments.stream()
                .map(comment -> CommentMapper
                        .toCommentDto(comment, eventName, userNames.get(comment.getAuthorId())))
                .toList();

        if (sortType == SortType.LIKES) {
            return commentsResult.stream().sorted(Comparator.comparing(CommentDto::getLikes).reversed()).toList();
        } else {
            return commentsResult.stream().sorted(Comparator.comparing(CommentDto::getCreated).reversed()).toList();
        }
    }

    @Transactional
    @Override
    public CommentDto addLike(Long userId, Long commentId) {
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Comment comment = checkComment(commentId);
        if (comment.getAuthorId().equals(userId)) {
            throw new ValidationException("Пользователь не может лайкать свой комментарий");
        }
        if (!comment.getLikes().add(userId)) {
            throw new ValidationException("Нельзя поставить лайк второй раз");
        }
        UserDto user = checkUser(comment.getAuthorId());
        EventFullDto event = checkEvent(comment.getEventId());

        return CommentMapper.toCommentDto(comment, event.getAnnotation(), user.getName());
    }

    @Transactional
    @Override
    public void deleteLike(Long userId, Long commentId) {
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        Comment comment = checkComment(commentId);
        if (!comment.getLikes().remove(userId)) {
            throw new NotFoundException("Пользователь не лайкал комментарий с id: " + commentId);
        }
    }

    @Override
    public CommentDto getComment(Long id) {
        Comment comment = checkComment(id);
        UserDto user = checkUser(comment.getAuthorId());
        EventFullDto event = checkEvent(comment.getEventId());
        return CommentMapper.toCommentDto(comment, event.getAnnotation(), user.getName());
    }

    @Override
    public void deleteCommentsOfUser(Long userId) {

    }


    @Transactional
    @Override
    public UserDtoForAdmin addBanCommited(Long userId, Long eventId) {
        checkEventId(eventId);
        UserDto user = checkUser(userId);
        if (!eventClient.checkExistsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }
        if (banRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new ValidationException("Уже добавлен такой запрет на комментирование");
        }
        banRepository.save(BanComment.builder()
                .userId(userId)
                .eventId(eventId)
                .build());
        return UserDtoForAdmin.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .forbiddenCommentEvents(banRepository.findAllByUserId(userId).stream()
                        .map(BanComment::getEventId)
                        .collect(Collectors.toSet()))
                .build();
    }

    @Transactional
    @Override
    public void deleteBanCommited(Long userId, Long eventId) {
        checkEventId(eventId);
        if (!userClient.checkExistsById(userId)) {
            throw new NotFoundException("Пользователь не найден");
        }
        if (!eventClient.checkExistsById(eventId)) {
            throw new NotFoundException("Событие не найдено");
        }
        if (!banRepository.existsByEventIdAndUserId(eventId, userId)) {
            throw new NotFoundException("Такого запрета на комментирование не найдено");
        }
        banRepository.deleteByEventIdAndUserId(eventId, userId);
    }

    private UserDto checkUser(Long userId) {
        return userClient.findById(userId);
    }

    private EventFullDto checkEvent(Long eventId) {
        return eventClient.findEventById(eventId);
    }

    private Comment checkComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
    }

    private void checkEventId(Long eventId) {
        if (eventId == 0) {
            throw new ValidationException("Не задан eventId");
        }
    }
}
