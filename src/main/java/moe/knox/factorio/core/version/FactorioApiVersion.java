package moe.knox.factorio.core.version;

import com.intellij.util.text.SemVer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public record FactorioApiVersion(String version, boolean latest) implements Comparable<FactorioApiVersion> {
    public static FactorioApiVersion createVersion(String version) {
        return new FactorioApiVersion(version, false);
    }

    public static FactorioApiVersion createLatestVersion(String version) {
        return new FactorioApiVersion(version, true);
    }

    @Override
    public String toString() {
        return version;
    }

    @Override
    public int compareTo(@NotNull FactorioApiVersion o) {
        SemVer verA = Objects.requireNonNull(SemVer.parseFromText(version));
        SemVer verB = Objects.requireNonNull(SemVer.parseFromText(o.version));

        return verA.compareTo(verB);
    }
}
