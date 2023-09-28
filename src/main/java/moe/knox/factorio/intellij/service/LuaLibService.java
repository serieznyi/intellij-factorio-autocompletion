package moe.knox.factorio.intellij.service;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import lombok.CustomLog;
import moe.knox.factorio.core.GettingTagException;
import moe.knox.factorio.core.LuaLibParser;
import moe.knox.factorio.core.NotificationService;
import moe.knox.factorio.core.PrototypesService;
import moe.knox.factorio.core.version.FactorioApiVersion;
import moe.knox.factorio.intellij.FactorioState;
import moe.knox.factorio.intellij.util.FilesystemUtil;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

@CustomLog
public class LuaLibService {
    private final LuaLibParser luaLibParser;
    private final Project project;
    private final AtomicBoolean downloadInProgress = new AtomicBoolean(false);
    private final FactorioState factorioState;

    private LuaLibService(Project project) {
        this.project = project;
        this.factorioState = FactorioState.getInstance(project);

        Path pluginDir = FilesystemUtil.getPluginDir();
        Path luaLibRootPath = pluginDir.resolve("lualib");
        Path corePrototypesRootPath = pluginDir.resolve("core_prototypes");
        luaLibParser = new LuaLibParser(luaLibRootPath, corePrototypesRootPath);
    }

    public static LuaLibService getInstance(Project project) {
        return new LuaLibService(project);
    }

    public Path getCurrentLuaLibPath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioApiVersion version = this.factorioState.selectedFactorioVersion;

        var path = luaLibParser.getLuaLibPath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
        }

        return path;
    }

    public Path getCurrentCorePrototypePath() {
        if (downloadInProgress.get()) {
            return null;
        }

        FactorioApiVersion version = this.factorioState.selectedFactorioVersion;

        var path = luaLibParser.getPrototypePath(version);

        if (path == null && downloadInProgress.compareAndSet(false, true)) {
            ProgressManager.getInstance().run(new LuaLibDownloadTask());
        }

        return path;
    }

    public void removeLuaLibFiles() {
        if (downloadInProgress.get()) {
            return;
        }

        luaLibParser.removeLuaLibFiles();
        PrototypesService.getInstance(project).reloadIndex();
    }

    public boolean checkForUpdate() {
        boolean needUpdate = false;

        try {
            FactorioApiVersion selectedVersion = this.factorioState.selectedFactorioVersion;

            needUpdate = luaLibParser.checkForUpdate(selectedVersion);

            if (needUpdate && downloadInProgress.compareAndSet(false, true)) {
                ProgressManager.getInstance().run(new LuaLibDownloadTask());
            }
        } catch (GettingTagException e) {
            log.error(e);
            NotificationService.getInstance(project).notifyErrorTagsDownloading();
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
                FactorioApiVersion selectedVersion = FactorioState.getInstance(project).selectedFactorioVersion;

                luaLibParser.downloadAll(selectedVersion);

                ApplicationManager.getApplication().invokeLater(() -> PrototypesService.getInstance(project).reloadIndex());
            } catch (GettingTagException e) {
                log.error(e);
                NotificationService.getInstance(project).notifyErrorTagsDownloading();
            } catch (IOException e) {
                log.error(e);
                NotificationService.getInstance(project).notifyErrorCreatingLuaLib();
            } finally {
                downloadInProgress.set(false);
            }
        }
    }
}
