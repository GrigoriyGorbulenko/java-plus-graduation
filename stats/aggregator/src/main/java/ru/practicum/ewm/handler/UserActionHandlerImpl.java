package ru.practicum.ewm.handler;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class UserActionHandlerImpl implements UserActionHandler {
    private final Map<Long, Map<Long, Double>> eventActions = new HashMap<>();
    private final Map<Long, Double> eventWeights = new HashMap<>();
    private final Map<Long, Map<Long, Double>> minWeightsSum = new HashMap<>();
    @Value("${application.action-weight.view}")
    private float viewMark;
    @Value("${application.action-weight.register}")
    private float registerMark;
    @Value("${application.action-weight.like}")
    private float likeMark;

    public List<EventSimilarityAvro> calcSimilarity(UserActionAvro action) {
        List<EventSimilarityAvro> similarity = new ArrayList<>();
        Long actionEventId = action.getEventId();
        Long actionUserId = action.getUserId();

        double delta = updateEventAction(action);

        if (delta > 0) {

            eventWeights.merge(actionEventId, delta, Double::sum);

            Set<Long> anotherEvents = eventActions.keySet().stream()
                    .filter(id -> !Objects.equals(id, actionEventId))
                    .collect(Collectors.toSet());

            anotherEvents.forEach(ae -> {
                double sim = getMinWeightsSum(actionEventId, ae, delta, actionUserId) /
                        (Math.sqrt(eventWeights.get(actionEventId)) * Math.sqrt(eventWeights.get(ae)));

                if (sim > 0.0) {
                    similarity.add(EventSimilarityAvro.newBuilder()
                            .setEventA(Math.min(actionEventId, ae))
                            .setEventB(Math.max(actionEventId, ae))
                            .setScore(sim)
                            .setTimestamp(Instant.now())
                            .build());
                }
            });
        }

        return similarity;
    }

    private Double updateEventAction(UserActionAvro action) {
        Long eventId = action.getEventId();
        Long userId = action.getUserId();
        Double newWeight = Double.valueOf(mapActionToWeight(action.getActionType()));

        Map<Long, Double> userActions = eventActions.computeIfAbsent(eventId, k -> new HashMap<>());
        Double oldWeight = userActions.get(userId);

        if (oldWeight == null || newWeight > oldWeight) {
            userActions.put(userId, newWeight);
            eventActions.put(eventId, userActions);
            double delta = oldWeight == null ? newWeight : newWeight - oldWeight;
            return delta;
        }
        return 0.0;
    }

    private Double getMinWeightsSum(Long eventA, Long eventB, Double delta, Long userId) {
        Long first = Math.min(eventA, eventB);
        Long second = Math.max(eventA, eventB);

        Double weightA = eventActions.get(eventA).get(userId);
        Double weightB = eventActions.get(eventB).get(userId);

        if (weightB == null) {
            return 0.0;
        }

        Map<Long, Double> innerMap = minWeightsSum.computeIfAbsent(first, k -> new HashMap<>());

        Double weight = innerMap.get(second);
        if (weight == null) {
            weight = calcMinWeightsSum(eventA, eventB);
            innerMap.put(eventB, weight);
            minWeightsSum.put(first, innerMap);
            return weight;
        }

        double newWeight;
        if (weightA > weightB && (weightA - delta) < weightB) {
            newWeight = weight + (weightB - (weightA - delta));
        } else if (weightA <= weightB) {
            newWeight = weight + delta;
        } else {
            return weight;
        }
        minWeightsSum.get(first).put(second, newWeight);
        return newWeight;
    }

    private Double calcMinWeightsSum(Long eventA, Long eventB) {
        List<Double> weights = new ArrayList<>();

        Map<Long, Double> userActionsA = eventActions.get(eventA);
        Map<Long, Double> userActionsB = eventActions.get(eventB);

        userActionsA.forEach((aUser, aWeight) -> {
            if (userActionsB.containsKey(aUser)) {
                weights.add(Math.min(aWeight, userActionsB.get(aUser)));
            }
        });

        if (weights.isEmpty()) {
            return 0.0;
        }
        return weights.stream().mapToDouble(Double::doubleValue).sum();
    }

    private Float mapActionToWeight(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> viewMark;
            case REGISTER -> registerMark;
            case LIKE -> likeMark;
        };
    }
}
