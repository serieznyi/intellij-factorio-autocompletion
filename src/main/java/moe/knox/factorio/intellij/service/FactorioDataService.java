package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import moe.knox.factorio.core.parser.luaLib.FactorioDataParser;
import moe.knox.factorio.intellij.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.version.FactorioVersion;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FileUtil;
import org.jetbrains.annotations.NotNull;

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
        Path luaLibRootPath = pluginDir.resolve("lualib");
        Path corePrototypesRootPath = pluginDir.resolve("core_prototypes");
        factorioDataParser = new FactorioDataParser(luaLibRootPath, corePrototypesRootPath);
    }

    public static FactorioDataService getInstance(Project project)
    {
        return new FactorioDataService(project);
    }

    public Path getCurrentLuaLibPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = factorioDataParser.getLuaLibPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
        }

        return path;
    }

    public Path getCurrentCorePrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioVersion version = FactorioState.getInstance(project).selectedFactorioVersion;

        var path = factorioDataParser.getPrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
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
            FactorioVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

            needUpdate = factorioDataParser.checkForUpdate(selectedVersion);

            if (needUpdate && downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new LuaLibDownloadTask());
            }
        } catch (Throwable e) {
            LOG.error(e);
            NotificationService.getInstance(project).notifyErrorLuaLibUpdating();
        }

        return needUpdate;
    }

    private class LuaLibDownloadTask extends Task.Backgroundable {
        public LuaLibDownloadTask() {
            super(project, "Download Factorio Lualib", false);
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
