package me.f0reach.holofans.lobbyconnector.velocity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class PlayerDataManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type DATA_TYPE = new TypeToken<Map<String, PlayerData>>() {}.getType();

    private final Path dataFile;
    private final Map<String, PlayerData> data = new ConcurrentHashMap<>();

    public static class PlayerData {
        public String lastServer;
        public boolean defaultSurvival;
    }

    public PlayerDataManager(Path dataDirectory) {
        this.dataFile = dataDirectory.resolve("playerdata.json");
    }

    public void load() throws IOException {
        if (Files.exists(dataFile)) {
            String json = Files.readString(dataFile);
            Map<String, PlayerData> loaded = GSON.fromJson(json, DATA_TYPE);
            if (loaded != null) {
                data.putAll(loaded);
            }
        }
    }

    public void save() throws IOException {
        Files.createDirectories(dataFile.getParent());
        Files.writeString(dataFile, GSON.toJson(data, DATA_TYPE));
    }

    private PlayerData getOrCreate(UUID uuid) {
        return data.computeIfAbsent(uuid.toString(), k -> new PlayerData());
    }

    public String getLastServer(UUID uuid) {
        PlayerData pd = data.get(uuid.toString());
        return pd != null ? pd.lastServer : null;
    }

    public void setLastServer(UUID uuid, String server) {
        getOrCreate(uuid).lastServer = server;
        saveAsync();
    }

    public boolean isDefaultSurvival(UUID uuid) {
        PlayerData pd = data.get(uuid.toString());
        return pd != null && pd.defaultSurvival;
    }

    public boolean toggleDefaultSurvival(UUID uuid) {
        PlayerData pd = getOrCreate(uuid);
        pd.defaultSurvival = !pd.defaultSurvival;
        saveAsync();
        return pd.defaultSurvival;
    }

    private void saveAsync() {
        try {
            save();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
