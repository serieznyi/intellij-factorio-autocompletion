package moe.knox.factorio.intellij;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.project.Project;
import com.intellij.util.xmlb.Converter;
import com.intellij.util.xmlb.XmlSerializerUtil;
import com.intellij.util.xmlb.annotations.OptionTag;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

@State(
        name = "FactorioAutocompletionConfig",
        storages = {
                @Storage("FactorioAutocompletionConfig.xml")
        }
)
public class FactorioState implements PersistentStateComponent<FactorioState> {
    public boolean integrationActive = false;
    @NotNull
    @OptionTag(converter = FactorioApiVersionConverter.class)
    public FactorioApiVersion selectedFactorioVersion;
    public boolean useLatestVersion = true;

    public FactorioState() throws IOException {
        // todo move in another method ?
        selectedFactorioVersion = (new ApiVersionResolver()).supportedVersions().latestVersion();
    }

    public static FactorioState getInstance(Project project) {
        return project.getService(FactorioState.class);
    }

    @Nullable
    @Override
    public FactorioState getState() {
        return this;
    }

    @Override
    public void loadState(@NotNull FactorioState state) {
        XmlSerializerUtil.copyBean(state, this);
    }

    private static class FactorioApiVersionConverter extends Converter<FactorioApiVersion> {
        @Override
        public @Nullable FactorioApiVersion fromString(@NotNull String value) {
            String[] parts = value.split(",");

            var version = parts[0];
            var latest = Boolean.parseBoolean(parts[1]);

            return latest ? FactorioApiVersion.createLatestVersion(version) : FactorioApiVersion.createVersion(version);
        }

        @Override
        public @Nullable String toString(@NotNull FactorioApiVersion value) {
            return value.version() + "," + value.latest();
        }
    }
}
