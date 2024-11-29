package me.lenglet;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DueDiligence {

    private static final State INITIAL_STATE = new State(null, null);
    private Long id;
    private State state;
    private List<DueDiligenceEvent> events;

    private DueDiligence(
            long id,
            State initialState
    ) {
        this.id = id;
        this.state = initialState;
        this.events = new ArrayList<>();
    }

    public DueDiligence(List<DueDiligenceEvent> events) {
        this.events = new ArrayList<>(events);
    }

    public static DueDiligence init(String clientId) {
        return new DueDiligence(
                List.of(
                        new DueDiligenceEvent.Created(
                                new DueDiligenceEvent.InitialDueDiligence(
                                        clientId,
                                        "standard"))));
    }

    public static DueDiligence from(long id, List<DueDiligenceEvent> events) {
        return events.stream()
                .reduce(new DueDiligence(id, INITIAL_STATE),
                        (DueDiligence::apply),
                        (_, _) -> {
                            throw new UnsupportedOperationException();
                        });
    }

    public long getId() {
        return this.id;
    }

    public List<DueDiligenceEvent> getEvents() {
        return List.copyOf(this.events);
    }

    public void process(DueDiligenceCommand command) {
        switch (command) {
            case DueDiligenceCommand.Patch patch -> {
                this.apply(new DueDiligenceEvent.Patched());
            }
        }
    }

    public DueDiligence apply(DueDiligenceEvent event) {
        final var dueDiligence = switch (event) {
            case DueDiligenceEvent.Created created -> applyCreated(created);
            case DueDiligenceEvent.Patched patched -> applyPatched(patched);
        };
        dueDiligence.events.add(event);
        return dueDiligence;
    }

    private DueDiligence applyPatched(DueDiligenceEvent.Patched patched) {
        return this;
    }

    private DueDiligence applyCreated(DueDiligenceEvent.Created created) {
        this.state.clientId = created.initialDueDiligence().clientId();
        this.state.classification = created.initialDueDiligence().classification();
        return this;
    }

    public static class State {
        private String clientId;
        private String classification;

        public State(
                String clientId,
                String classification
        ) {
            this.clientId = clientId;
            this.classification = classification;
        }
    }
}
