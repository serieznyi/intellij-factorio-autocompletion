package moe.knox.factorio.core.parser.factorioData;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.io.FileUtil;
import moe.knox.factorio.core.version.FactorioVersion;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final public class FactorioDataParser {
    private static final Logger LOG = Logger.getInstance(FactorioDataParser.class);
    private static final String luaLibGithubTagsZipLink = "https://api.github.com/repos/wube/factorio-data/zipball";

    private final Path rootPath;

    public FactorioDataParser(@NotNull Path rootPath) {
        this.rootPath = rootPath;
    }

    public void removeLibraryFiles() {
        FileUtil.delete(rootPath.toFile());
    }

    public @NotNull Path getLibraryPath(FactorioVersion version) {
        return rootPath.resolve(version.version());
    }

    public @NotNull Path getLuaLibPath(FactorioVersion version) {
        return rootPath.resolve(version.version()).resolve("core/lualib");
    }

    public @NotNull Path getCorePrototypePath(FactorioVersion version) {
        return rootPath.resolve(version.version()).resolve("core/prototypes");
    }

    public void downloadAll(FactorioVersion version) throws IOException {
        try {
            Path versionRoot = rootPath.resolve(version.version());
            Path baseRoot = versionRoot.resolve("base");
            Path coreRoot = versionRoot.resolve("core");

            Files.createDirectories(baseRoot);
            Files.createDirectories(coreRoot);

            URL url = new URL(luaLibGithubTagsZipLink + "/" + version.version());
            try (ZipInputStream zipInputStream = new ZipInputStream(url.openStream())) {
                ZipEntry zipEntry;
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    saveZipEntry(zipInputStream, zipEntry, "/base/", baseRoot);
                    saveZipEntry(zipInputStream, zipEntry, "/core/", coreRoot);
                }
            }
        } catch (IOException e) {
            removeLibraryFiles();

            throw e;
        }
    }

    private void saveZipEntry(ZipInputStream zipInputStream, ZipEntry zipEntry, String inZipDir, Path toSaveDir) throws IOException {
        int pos = zipEntry.getName().lastIndexOf(inZipDir);

        if (pos == -1) {
            return;
        }

        String filename = zipEntry.getName().substring(pos + inZipDir.length());
        if (filename.isEmpty()) {
            return;
        }

        Path path = toSaveDir.resolve(filename);

        if (zipEntry.isDirectory()) {
            Files.createDirectories(path);
            return;
        }

        // save file
        byte[] buffer = new byte[2048];
        try (FileOutputStream fos = new FileOutputStream(path.toFile());
             BufferedOutputStream bos = new BufferedOutputStream(fos, buffer.length)) {

            int len;
            while ((len = zipInputStream.read(buffer)) > 0) {
                bos.write(buffer, 0, len);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    /**
     * When an update is available it will also remove the old one and start the download of the new one.
     *
     * @param version
     * @return true when an update is available or the API not existent
     */
    public boolean checkForUpdate(FactorioVersion version) {
        return !rootPath.resolve(version.version()).toFile().exists();
    }
}
