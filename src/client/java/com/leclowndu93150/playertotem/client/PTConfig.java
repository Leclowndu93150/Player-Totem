package com.leclowndu93150.playertotem.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PTConfig {

    private static final Path CONFIG_PATH = Paths.get("config", "playertotem.json");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private boolean armsMove = true;

    public PTConfig() {
        loadConfig();
    }

    /**
     * Gets the value of armsMove.
     *
     * @return true if arms move, false otherwise.
     */
    public boolean canMoveArms() {
        return armsMove;
    }

    /**
     * Loads the configuration from the JSON file or creates a default one if it doesn't exist.
     */
    public void loadConfig() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                JsonObject configObject = GSON.fromJson(json, JsonObject.class);
                if (configObject.has("armsMove")) {
                    armsMove = configObject.get("armsMove").getAsBoolean();
                }
            } catch (IOException e) {
                System.err.println("Failed to load config: " + e.getMessage());
                saveDefaultConfig();
            }
        } else {
            saveDefaultConfig();
        }
    }

    /**
     * Saves the default configuration to the JSON file.
     */
    private void saveDefaultConfig() {
        JsonObject defaultConfig = new JsonObject();
        defaultConfig.addProperty("armsMove", armsMove);

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, GSON.toJson(defaultConfig));
        } catch (IOException e) {
            System.err.println("Failed to save default config: " + e.getMessage());
        }
    }
}
