package com.swiftfaze.veil.entities.player;

import java.util.LinkedHashMap;
import java.util.Map;

public class QuestLog {

    public enum State {
        NOT_STARTED, OFFERED, ACTIVE, COMPLETE
    }

    private final Map<String, State> statesByQuestId = new LinkedHashMap<>();

    public State getState(String questId) {
        return statesByQuestId.getOrDefault(questId, State.NOT_STARTED);
    }

    public void setState(String questId, State state) {
        statesByQuestId.put(questId, state);
    }
}
