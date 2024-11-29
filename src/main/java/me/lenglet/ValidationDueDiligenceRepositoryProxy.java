package me.lenglet;

import jakarta.validation.ValidatorFactory;

public class ValidationDueDiligenceRepositoryProxy implements DueDiligenceRepository {

    private final DueDiligenceRepository dueDiligenceRepository;
    private final ValidatorFactory validatorFactory;

    public ValidationDueDiligenceRepositoryProxy(
            DueDiligenceRepository dueDiligenceRepository,
            ValidatorFactory validatorFactory
    ) {
        this.dueDiligenceRepository = dueDiligenceRepository;
        this.validatorFactory = validatorFactory;
    }

    @Override
    public DueDiligence findById(long id) {
        return this.dueDiligenceRepository.findById(id);
    }

    @Override
    public void update(DueDiligence dueDiligence) {
        validate(dueDiligence);
        this.dueDiligenceRepository.update(dueDiligence);
    }

    @Override
    public void persist(DueDiligence dueDiligence) {

    }

    private void validate(DueDiligence dueDiligence) {
        final var constraintViolations = this.validatorFactory.getValidator().validate(dueDiligence);
        if (constraintViolations.isEmpty()) {
            return;
        }
        throw new RuntimeException();
    }
}
