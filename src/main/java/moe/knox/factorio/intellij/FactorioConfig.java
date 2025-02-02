package moe.knox.factorio.intellij;

import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import moe.knox.factorio.intellij.service.ApiService;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.service.FactorioDataService;
import moe.knox.factorio.intellij.service.PrototypeService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.io.IOException;
import java.util.Comparator;
import java.util.Objects;

public class FactorioConfig implements SearchableConfigurable {
    private final ApiVersionResolver apiVersionResolver;
    @NotNull
    private final FactorioApiVersion latestExistingVersion;
    Project project;
    private final FactorioState config;
    private JPanel rootPanel;
    private JCheckBox enableFactorioIntegrationCheckBox;
    private JComboBox<DropdownVersion> selectApiVersion;
    private JLabel loadError;
    private JButton reloadButton;
    private JLabel selectApiVersionLabel;

    public FactorioConfig(@NotNull Project project) throws IOException {
        this.project = project;
        config = FactorioState.getInstance(project);
        apiVersionResolver = new ApiVersionResolver();
        latestExistingVersion = apiVersionResolver.supportedVersions().latestVersion();

        enableFactorioIntegrationCheckBox.setSelected(config.integrationActive);


        try {
            var latestDropdownVersion = DropdownVersion.createLatest();

            selectApiVersion.addItem(latestDropdownVersion);
            apiVersionResolver
                    .supportedVersions()
                    .stream()
                    .sorted(Comparator.reverseOrder())
                    .map(DropdownVersion::fromApiVersion)
                    .forEach(v -> selectApiVersion.addItem(v))
            ;

            if (config.useLatestVersion) {
                selectApiVersion.setSelectedItem(latestDropdownVersion);
            } else {
                selectApiVersion.setSelectedItem(DropdownVersion.fromApiVersion(config.selectedFactorioVersion));
            }

            // hide error message
            selectApiVersion.setEnabled(true);
            loadError.setVisible(false);
            enableFactorioIntegrationCheckBox.setEnabled(true);
            reloadButton.setEnabled(config.integrationActive);
        }
        // todo catch only connection problems and wrote correct message
        catch (Exception e) {
            // show error message
            selectApiVersion.setEnabled(false);
            loadError.setText("Error loading Factorio versions. You need to have active internet connection to change these settings");
            loadError.setVisible(true);
            enableFactorioIntegrationCheckBox.setEnabled(false);
            reloadButton.setEnabled(false);
        }

        reloadButton.addActionListener(actionEvent -> {
            removeParsedLibraries();
            updateLibraries();
        });
    }

    @NotNull
    @Override
    public String getId() {
        return "preference.FactorioCompletionConfig";
    }

    @Override
    public @NlsContexts.ConfigurableName String getDisplayName() {
        return "Factorio Autocompletion";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        return rootPanel;
    }

    @Override
    public boolean isModified() {
        if (!selectApiVersion.isEnabled()) {
            return false;
        }

        return config.integrationActive != enableFactorioIntegrationCheckBox.isSelected()
                || !config.selectedFactorioVersion.equals(getSelectedVersion());
    }

    @Override
    public void apply() {
        if (isIntegrationTurnedOff()) {
            removeParsedLibraries();
        }

        if (isVersionChanged()) {
            ApiService.getInstance(project).removeLibraryFiles();
            FactorioDataService.getInstance(project).removeLibraryFiles();
            FactorioDataService.getInstance(project).checkForUpdate();
        }

        reloadButton.setEnabled(enableFactorioIntegrationCheckBox.isSelected());

        config.integrationActive = enableFactorioIntegrationCheckBox.isSelected();
        config.useLatestVersion = isUseLatestVersion();
        config.selectedFactorioVersion = getSelectedVersion();

        WriteAction.run(FactorioLibraryProvider::reload);
    }

    private void removeParsedLibraries() {
        ApiService.getInstance(project).removeLibraryFiles();
        PrototypeService.getInstance(project).removeLibraryFiles();
        FactorioDataService.getInstance(project).removeLibraryFiles();
    }

    private void updateLibraries() {
        ApiService.getInstance(project).checkForUpdate();
        PrototypeService.getInstance(project).checkForUpdate();
        FactorioDataService.getInstance(project).checkForUpdate();
    }

    private boolean isUseLatestVersion() {
        return Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem()).isLatest();
    }

    private FactorioApiVersion getSelectedVersion() {
        var dropdownVersion = Objects.requireNonNull((DropdownVersion) selectApiVersion.getSelectedItem());

        if (dropdownVersion.isLatest()) {
            return latestExistingVersion;
        }

        return FactorioApiVersion.createVersion(dropdownVersion.version);
    }

    private boolean isVersionChanged() {
        return !config.selectedFactorioVersion.equals(getSelectedVersion());
    }

    private boolean isIntegrationTurnedOff() {
        return config.integrationActive && !enableFactorioIntegrationCheckBox.isSelected();
    }

    private record DropdownVersion(String version, String name) {
        public static DropdownVersion createLatest() {
            return new DropdownVersion("latest", "Latest version");
        }

        public static DropdownVersion fromApiVersion(FactorioApiVersion v) {
            return new DropdownVersion(v.version(), v.version());
        }

        public boolean isLatest() {
            return version.equals("latest");
        }

        @Override
        public String toString() {
            return name;
        }
    }
}
