package me.lenglet;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public class DueDiligence {

    private long id;
    private long version;
    private Data data;

    public DueDiligence(long id, long version, Data data) {
        this.id = id;
        this.version = version;
        this.data = data;
    }

    public long getId() {
        return this.id;
    }

    static class Data {

        @NotNull
        private String clientId;
        @Valid
        private Set<Relationship> relationships;

    }

    static class Relationship {

        @NotNull
        private String name;
        @NotNull
        private String type;
    }
}
