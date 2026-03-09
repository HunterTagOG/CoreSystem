package dev.huntertagog.coresystem.fabric.common.rct;

import com.gitlab.srcmc.rctapi.api.RCTApi;
import com.gitlab.srcmc.rctapi.api.errors.RCTException;
import com.gitlab.srcmc.rctapi.api.models.TrainerModel;
import com.google.gson.Gson;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.fabric.CoresystemCommon;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public final class RctBootstrap {

    private static final Logger LOG = LoggerFactory.get("RCT");
    private static final String MOD_ID = CoresystemCommon.MOD_ID;

    private static final RCTApi RCT = RCTApi.initInstance(MOD_ID);
    private static final Gson GSON = RCT.gsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    private RctBootstrap() {
    }

    public static RCTApi api() {
        return RCT;
    }

    public static void register() {
        ServerLifecycleEvents.SERVER_STARTING.register(RctBootstrap::onServerStarting);
    }

    private static void onServerStarting(MinecraftServer server) {
        var registry = RCT.getTrainerRegistry();
        registry.init(server);

        File trainerDir = server.getRunDirectory()
                .resolve("config")
                .resolve("coresystem")
                .resolve("trainers")
                .toFile();

        if (!trainerDir.exists()) {
            trainerDir.mkdirs();
            LOG.info("Created trainer directory at {}", trainerDir.getAbsolutePath());
            return;
        }

        File[] files = trainerDir.listFiles(f -> f.getName().toLowerCase().endsWith(".json"));
        if (files == null || files.length == 0) {
            LOG.info("No trainer files found in {}", trainerDir.getAbsolutePath());
            return;
        }

        for (File trainerFile : files) {
            String trainerId = fileToId(trainerFile);
            try (BufferedReader rd = new BufferedReader(new FileReader(trainerFile))) {
                TrainerModel model = GSON.fromJson(rd, TrainerModel.class);
                registry.registerNPC(trainerId, model);
                LOG.info("Registered trainer NPC '{}' from {}", trainerId, trainerFile.getName());
            } catch (RCTException errors) {
                LOG.warn("Trainer model validation issues in {}", trainerFile.getName());
                errors.getErrors().forEach(err -> LOG.warn(err.message));
            } catch (Exception e) {
                LOG.error("Failed to register trainer from {}", trainerFile.getName(), e);
            }
        }
    }

    private static String fileToId(File file) {
        String name = file.getName().toLowerCase().trim();
        int i = name.lastIndexOf('.');
        return (i < 0 ? name : name.substring(0, i)).replace(' ', '_');
    }
}
