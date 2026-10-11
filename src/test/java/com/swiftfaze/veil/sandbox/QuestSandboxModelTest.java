package com.swiftfaze.veil.sandbox;

import com.swiftfaze.veil.entities.quests.Quest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class QuestSandboxModelTest {

    private static final int KILL_COUNT = 3;

    private static Quest quest(String id, String name) {
        return new Quest(id, name, new Quest.Objective("kill", "core:goblin", KILL_COUNT), List.of());
    }

    @Test
    void findByIdReturnsTheMatchingQuestNotTheFirstOne() {
        QuestSandboxModel model = new QuestSandboxModel(List.of(quest("core:a", "A"), quest("core:b", "B")));

        assertEquals("B", model.findById("core:b").getName());
    }
}
