package com.swiftfaze.veil.entities.player;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestLogTest {

    @Test
    void defaultStateForUnknownQuestIsNotStarted() {
        QuestLog questLog = new QuestLog();

        assertEquals(QuestLog.State.NOT_STARTED, questLog.getState("core:goblin_slayer"));
    }

    @Test
    void settingStateChangesIt() {
        QuestLog questLog = new QuestLog();

        questLog.setState("core:goblin_slayer", QuestLog.State.ACTIVE);

        assertEquals(QuestLog.State.ACTIVE, questLog.getState("core:goblin_slayer"));
    }

    @Test
    void stateCanBeOverwrittenWithoutOrderValidation() {
        QuestLog questLog = new QuestLog();
        questLog.setState("core:goblin_slayer", QuestLog.State.COMPLETE);

        questLog.setState("core:goblin_slayer", QuestLog.State.NOT_STARTED);

        assertEquals(QuestLog.State.NOT_STARTED, questLog.getState("core:goblin_slayer"));
    }

    @Test
    void tracksMultipleQuestsIndependently() {
        QuestLog questLog = new QuestLog();
        questLog.setState("core:goblin_slayer", QuestLog.State.ACTIVE);

        assertEquals(QuestLog.State.NOT_STARTED, questLog.getState("core:explorer"));
    }
}
