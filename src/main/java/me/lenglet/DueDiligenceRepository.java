package me.lenglet;

public interface DueDiligenceRepository {

    DueDiligence findById(long id);
    void update(DueDiligence dueDiligence);
}
