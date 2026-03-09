package dev.huntertagog.coresystem.common.world.structures.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

public final class StructureSpawnConfigLoader {

    private static final Logger LOG = LoggerFactory.get("StructureSpawnConfigLoader");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private static final String FILE_NAME = "structure_spawns.json";
    private static final String DEFAULT_RESOURCE =
            "/assets/coresystem/defaults/structure_spawns.json";

    private StructureSpawnConfigLoader() {
    }

    public static StructureSpawnConfig load(File configDir) {
        if (!configDir.exists()) configDir.mkdirs();

        File configFile = new File(configDir, FILE_NAME);

        // ---------------------------------------------
        // 1) Falls Datei nicht existiert → Default kopieren
        // ---------------------------------------------
        if (!configFile.exists()) {
            try (InputStream in = StructureSpawnConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
                if (in == null) {
                    throw new IOException("Default config resource not found: " + DEFAULT_RESOURCE);
                }

                Files.copy(in, configFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                System.out.println("[CoreSystem] Created default structure_spawns.json");
            } catch (Exception e) {
                LOG.error("Failed to create default structure_spawns.json: {}", e.getMessage());
            }
        }

        // ---------------------------------------------
        // 2) Datei lesen
        // ---------------------------------------------
        try (FileReader reader = new FileReader(configFile, StandardCharsets.UTF_8)) {
            StructureSpawnConfig cfg = GSON.fromJson(reader, StructureSpawnConfig.class);
            if (cfg.groups == null) cfg.groups = new java.util.ArrayList<>();
            return cfg;
        } catch (Exception e) {
            LOG.error("Failed to load default structure_spawns.json: {}", e.getMessage());
            return new StructureSpawnConfig();
        }
    }
}
