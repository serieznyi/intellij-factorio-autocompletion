package moe.knox.factorio.core;

import moe.knox.factorio.core.version.ApiVersionResolver;
import moe.knox.factorio.core.version.FactorioApiVersion;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.CleanupMode;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

public class LuaLibParserTest {
    private static Path tempDir;

    private LuaLibParser luaLibParser;

    @BeforeAll
    protected static void setUpAll(@TempDir(cleanup = CleanupMode.NEVER) Path tempDirArg)
    {
        tempDir = tempDirArg;
    }

    @BeforeEach
    protected void setUp() {
        Path luaLibRootPath = tempDir.resolve("lualib");
        Path corePrototypesRootPath = tempDir.resolve("core_prototypes");

        luaLibParser = new LuaLibParser(luaLibRootPath, corePrototypesRootPath);
    }

    public static Set<FactorioApiVersion> providerVersions() throws IOException {
        return (new ApiVersionResolver()).supportedVersions();
    }

    @ParameterizedTest
    @MethodSource("providerVersions")
    void downloadAll(FactorioApiVersion version) throws GettingTagException, IOException {
        luaLibParser.downloadAll(version);
    }
}