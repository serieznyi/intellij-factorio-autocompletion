package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import moe.knox.factorio.core.parser.factorioData.FactorioDataParser;
import moe.knox.factorio.intellij.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FileUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

public class FactorioDataService {
    private static final Logger LOG = Logger.getInstance(FactorioDataService.class);
    private final FactorioDataParser factorioDataParser;
    private final Project project;
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);

    private FactorioDataService(Project project) {
        this.project = project;

        Path pluginDir = FileUtil.getPluginDir();
        Path factorioDataRootPath = pluginDir.resolve("factorio_data");
        factorioDataParser = new FactorioDataParser(factorioDataRootPath);
    }

    public static FactorioDataService getInstance(Project project)
    {
        return new FactorioDataService(project);
    }

    public @Nullable Path getLuaLibPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = factorioDataParser.getLuaLibPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new FactorioDataTask());
        }

        return path;
    }

    public @Nullable Path getCorePrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = factorioDataParser.getCorePrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new FactorioDataTask());
        }

        return path;
    }

    public @Nullable VirtualFile getLibraryPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var libraryPath = factorioDataParser.getLibraryPath(version);
        var libraryExists = Files.exists(libraryPath);

        if (!libraryExists && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new FactorioDataTask());
        }

        return libraryExists ? VfsUtil.findFileByIoFile(libraryPath.toFile(), false): null;
    }

    public void removeLibraryFiles() {
        if (downloadInProgress.get()) {
            return;
        }

        factorioDataParser.removeLibraryFiles();
        PrototypesService.getInstance(project).reloadIndex();
    }

    public boolean checkForUpdate() {
        boolean needUpdate = false;

        try {
            FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

            needUpdate = factorioDataParser.checkForUpdate(selectedVersion);

            if (needUpdate && downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new FactorioDataTask());
            }
        } catch (Throwable e) {
            LOG.error(e);
            NotificationService.getInstance(project).notifyErrorLuaLibUpdating();
        }

        return needUpdate;
    }

    private class FactorioDataTask extends Task.Backgroundable {
        public FactorioDataTask() {
            super(project, "Download Factorio Data", false);
        }

        @Override
        public void run(@NotNull ProgressIndicator indicator) {
            try {
                FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                factorioDataParser.downloadAll(selectedVersion);

                ApplicationManager.getApplication().invokeLater(() -> PrototypesService.getInstance(project).reloadIndex());
            } catch (Throwable e) {
                LOG.error(e);
                NotificationService.getInstance(project).notifyErrorLuaLibUpdating();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}
