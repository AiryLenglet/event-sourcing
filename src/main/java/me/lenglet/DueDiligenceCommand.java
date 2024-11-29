package me.lenglet;

public sealed interface DueDiligenceCommand {

    record Patch(
    ) implements DueDiligenceCommand {
    }
}
