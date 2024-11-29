package me.lenglet;

public sealed interface DueDiligenceEvent {

    record Created(
            InitialDueDiligence initialDueDiligence
    ) implements DueDiligenceEvent {
    }

    record Patched(
    ) implements DueDiligenceEvent {
    }

    record InitialDueDiligence(
            String clientId,
            String classification
    ) {
    }
}
