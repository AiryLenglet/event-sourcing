package me.lenglet.service;

import me.lenglet.DueDiligence;
import me.lenglet.DueDiligenceRepository;
import me.lenglet.Transactional;

public class CreateDueDiligenceService {

    private final DueDiligenceRepository dueDiligenceRepository;

    public CreateDueDiligenceService(
            DueDiligenceRepository dueDiligenceRepository
    ) {
        this.dueDiligenceRepository = dueDiligenceRepository;
    }

    @Transactional
    public void execute(Request request) {
        final var dueDiligence = DueDiligence.init("435454");
        this.dueDiligenceRepository.persist(dueDiligence);
    }

    public record Request(
            String clientId
    ) {
    }
}
