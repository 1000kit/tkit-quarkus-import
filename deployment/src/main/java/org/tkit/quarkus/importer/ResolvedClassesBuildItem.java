package org.tkit.quarkus.importer;

import java.util.Map;

import io.quarkus.builder.item.SimpleBuildItem;
import lombok.Getter;

public final class ResolvedClassesBuildItem extends SimpleBuildItem {
    @Getter
    private final Map<String, Class<?>> classes;

    public ResolvedClassesBuildItem(Map<String, Class<?>> classes) {
        this.classes = classes;
    }
}
