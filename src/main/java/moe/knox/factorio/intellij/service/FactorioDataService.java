package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.CustomLog;
import moe.knox.factorio.core.parser.data.FactorioDataParser;
import moe.knox.factorio.intellij.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class FactorioDataService {
    private final FactorioDataParser factorioDataParser;
    private final Project project;
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final FactorioState factorioState;

    private FactorioDataService(Project project) {
        this.project = project;
        this.factorioState = FactorioState.getInstance(project);

        Path pluginDir = FilesystemUtil.getPluginDir();
        Path factorioDataRootPath = pluginDir.resolve("factorio_data");
        factorioDataParser = new FactorioDataParser(factorioDataRootPath);
    }

    public static FactorioDataService getInstance(Project project) {
        return new FactorioDataService(project);
    }

    public Path getCurrentLuaLibPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioApiVersion version = this.factorioState.selectedFactorioVersion;

        var path = factorioDataParser.getLuaLibPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new FactorioDataTask());
        }

        return path;
    }

    public Path getCurrentCorePrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioApiVersion version = this.factorioState.selectedFactorioVersion;

        var path = factorioDataParser.getPrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new FactorioDataTask());
        }

        return path;
    }

    public void removeLibraryFiles() {
        if (downloadInProgress.get()) {
            return;
        }

        factorioDataParser.removeLuaLibFiles();
        PrototypesService.getInstance(project).reloadIndex();
    }

    public boolean checkForUpdate() {
        boolean needUpdate = false;

        try {
            FactorioApiVersion selectedVersion = this.factorioState.selectedFactorioVersion;

            needUpdate = factorioDataParser.checkForUpdate(selectedVersion);

            if (needUpdate && downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioDataTask());
            }
        } catch (Throwable e) {
            log.error(e);
            NotificationService.getInstance(project).notifyErrorLuaLibUpdating();
        }

        return needUpdate;
    }

    private class FactorioDataTask extends Task.Backgroundable {
        public FactorioDataTask() {
            super(project, "Download Factorio Lualib", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioApiVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                factorioDataParser.downloadAll(selectedVersion);

                ApplicationManager.getApplication().invokeLater(() -> PrototypesService.getInstance(project).reloadIndex());
            } catch (Throwable e) {
                log.error(e);
                NotificationService.getInstance(project).notifyErrorLuaLibUpdating();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}
