package com.swiftfaze.veil.game;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TurnSchedulerTest {
    private static final String SPAWNER = "spawner";

    @Test
    void newSchedulerHasNoTurnsAndNoRegistrations() {
        TurnScheduler scheduler = new TurnScheduler();

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, scheduler.registeredCount());
    }

    @Test
    void advanceIncrementsTheTurnCounter() {
        TurnScheduler scheduler = new TurnScheduler();

        scheduler.advance();
        scheduler.advance();

        assertEquals(2, scheduler.getTurn());
    }

    @Test
    void advanceStepsEveryRegisteredTickableWithTheNewTurnNumber() {
        TurnScheduler scheduler = new TurnScheduler();
        List<Integer> turnsSeen = new ArrayList<>();
        scheduler.register(turnsSeen::add);

        scheduler.advance();
        scheduler.advance();

        assertEquals(List.of(1, 2), turnsSeen);
    }

    @Test
    void tickablesAreSteppedInRegistrationOrder() {
        TurnScheduler scheduler = new TurnScheduler();
        List<String> order = new ArrayList<>();
        scheduler.register(turn -> order.add("first"));
        scheduler.register(turn -> order.add("second"));

        scheduler.advance();

        assertEquals(List.of("first", "second"), order);
    }

    @Test
    void tickableRegisteredDuringATickIsNotSteppedUntilTheNextTurn() {
        TurnScheduler scheduler = new TurnScheduler();
        List<String> order = new ArrayList<>();
        scheduler.register(turn -> {
            order.add(SPAWNER);
            if (turn == 1) {
                scheduler.register(later -> order.add("spawned@" + later));
            }
        });

        scheduler.advance();
        assertEquals(List.of(SPAWNER), order);

        scheduler.advance();
        assertEquals(List.of(SPAWNER, SPAWNER, "spawned@2"), order);
    }

    @Test
    void registeredCountReflectsEachRegistration() {
        TurnScheduler scheduler = new TurnScheduler();

        scheduler.register(turn -> { });
        scheduler.register(turn -> { });

        assertEquals(2, scheduler.registeredCount());
    }

    @Test
    void resetClearsRegistrationsAndTheTurnCounter() {
        TurnScheduler scheduler = new TurnScheduler();
        scheduler.register(turn -> { });
        scheduler.advance();

        scheduler.reset();

        assertEquals(0, scheduler.getTurn());
        assertEquals(0, scheduler.registeredCount());
    }

    @Test
    void afterResetNothingIsSteppedOnTheNextAdvance() {
        TurnScheduler scheduler = new TurnScheduler();
        List<Integer> turnsSeen = new ArrayList<>();
        scheduler.register(turnsSeen::add);
        scheduler.reset();

        scheduler.advance();

        assertEquals(List.of(), turnsSeen);
        assertEquals(1, scheduler.getTurn());
    }
}
