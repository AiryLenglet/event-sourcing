package me.lenglet.service;

import me.lenglet.DueDiligenceCommand;
import me.lenglet.DueDiligenceRepository;
import me.lenglet.Transactional;

public class PatchDueDiligenceService {

    private final DueDiligenceRepository dueDiligenceRepository;

    public PatchDueDiligenceService(
            DueDiligenceRepository dueDiligenceRepository
    ) {
        this.dueDiligenceRepository = dueDiligenceRepository;
    }

    @Transactional
    public void execute(long id) {
        final var dueDiligence = this.dueDiligenceRepository.findById(id);
        dueDiligence.process(new DueDiligenceCommand.Patch());
        this.dueDiligenceRepository.update(dueDiligence);
    }
}
