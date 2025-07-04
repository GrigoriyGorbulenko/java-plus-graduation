package ru.practicum.ewm.comment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.comment.dto.CommentDto;
import ru.practicum.ewm.comment.dto.NewCommentDto;
import ru.practicum.ewm.comment.mapper.CommentMapper;
import ru.practicum.ewm.comment.model.Comment;
import ru.practicum.ewm.comment.repository.CommentRepository;
import ru.practicum.ewm.event.model.Event;
import ru.practicum.ewm.event.repository.EventRepository;
import ru.practicum.ewm.exception.NotFoundException;
import ru.practicum.ewm.exception.ValidationException;
import ru.practicum.ewm.user.model.User;
import ru.practicum.ewm.user.repository.UserRepository;


import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;

    @Transactional
    @Override
    public CommentDto createComment(Long eventId, Long userId, NewCommentDto newCommentDto) {
        Event event = checkEvent(eventId);
        User user = checkUser(userId);
        Comment comment = CommentMapper.toComment(newCommentDto, event, user);
        return CommentMapper.toCommentDto(commentRepository.save(comment));
    }

    @Transactional
    @Override
    public CommentDto updateComment(Long userId, Long commentId, NewCommentDto newCommentDto) {
        User user = checkUser(userId);
        Comment comment = checkComment(commentId);
        if(user.getId().equals(comment.getAuthor().getId())) {
            comment.setText(newCommentDto.getText());
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }


        return CommentMapper.toCommentDto(comment);
    }

    @Transactional
    @Override
    public void deleteComment(Long userId, Long commentId) {
        User user = checkUser(userId);
        Comment comment = checkComment(commentId);
        if(user.getId().equals(comment.getAuthor().getId())) {
            commentRepository.deleteById(commentId);
        } else {
            throw new ValidationException("Пользователь не оставлял комментарий с указанным Id " + commentId);
        }
    }

    @Override
    public List<CommentDto> getAllComments(Long eventId) {
        return commentRepository.findAllByEvent_Id(eventId)
                .stream()
                .map(CommentMapper::toCommentDto)
                .toList();
    }

    private User checkUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь не найден"));
    }

    private Event checkEvent(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Событие не найдено"));
    }

    private Comment checkComment(Long commentId) {
        return commentRepository.findById(commentId)
                .orElseThrow(() -> new NotFoundException("Комментарий не найден"));
    }

}
