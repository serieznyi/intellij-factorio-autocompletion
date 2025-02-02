package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.CustomLog;
import moe.knox.factorio.intellij.NotificationService;
import moe.knox.factorio.core.parser.api.ApiParser;
import moe.knox.factorio.core.version.ApiVersionCollection;
import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.FactorioLibraryProvider;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class ApiService {
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final Project project;
    private final ApiParser apiParser;

    private ApiService(Project project) {
        this.project = project;

        Path pluginDir = FilesystemUtil.getPluginDir();
        Path apiRootPath = pluginDir.resolve("factorio_api");
        apiParser = new ApiParser(apiRootPath);
    }

    public static ApiService getInstance(Project project)
    {
        return new ApiService(project);
    }

    public void removeLibraryFiles() {
        if (downloadInProgress.get()) {
            return;
        }

        apiParser.removeCurrentAPI();
        FactorioLibraryProvider.reload();
    }

    public void checkForUpdate() {
        FactorioState config = FactorioState.getInstance(project);

        if (!config.useLatestVersion) {
            return;
        }

        FactorioApiVersion newestVersion = detectLatestAllowedVersion();

        if (newestVersion != null && !newestVersion.equals(config.selectedFactorioVersion)) {
            removeLibraryFiles();

            if (downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new ApiTask());
            }
        }
    }

    public Optional<Path> getApiPath() {
        if (downloadInProgress.get()) {
            return Optional.empty();
        }

        FactorioApiVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        Optional<Path> path = apiParser.getApiPath(version);

        if (path.isEmpty() && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new ApiTask());
        }

        return path;
    }

    private FactorioApiVersion detectLatestAllowedVersion() {
        ApiVersionCollection factorioApiVersions;

        try {
            factorioApiVersions = (new ApiVersionResolver()).supportedVersions();
        } catch (Exception e) {
            log.error(e);
            NotificationService.getInstance(project).notifyErrorApiUpdating();
            return null;
        }

        return factorioApiVersions.latestVersion();
    }

    private class ApiTask extends Task.Backgroundable {
        public ApiTask() {
            super(project, "Download and Parse Factorio API", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioApiVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                apiParser.parse(selectedVersion);

                ApplicationManager.getApplication().invokeLater(FactorioLibraryProvider::reload);
            } catch (Throwable e) {
                log.error(e);
                NotificationService.getInstance(project).notifyErrorApiUpdating();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}
