/**
 Autotip - tips online boosters, and other online Autotip users.
 You send and receive tips, increasing your coin and experience gain.
 Copyright (C) 2026 QWERTZ, Sk1erLLC

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **/

package club.sk1er.mods.autotip.config;

import club.sk1er.mods.autotip.Autotip;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .create();

    private final Path configFile;

    private MessageMode messageMode = MessageMode.OFF;

    public Config() {
        Path configDir = FabricLoader.getInstance().getConfigDir().resolve("autotip");
        this.configFile = configDir.resolve("config.json");
        load();
    }

    public MessageMode getMessageMode() {
        return messageMode;
    }

    public void setMessageMode(MessageMode mode) {
        this.messageMode = mode;
        save();
    }

    public void save() {
        try {
            Files.createDirectories(configFile.getParent());
            ConfigData data = new ConfigData(messageMode.name().toLowerCase());
            Files.writeString(configFile, GSON.toJson(data));
        } catch (IOException e) {
            Autotip.getInstance().getMessageUtil().log("Failed to save config: " + e.getMessage());
        }
    }

    private void load() {
        if (Files.exists(configFile)) {
            try {
                String json = Files.readString(configFile);
                ConfigData data = GSON.fromJson(json, ConfigData.class);
                if (data != null && data.messageMode != null) {
                    MessageMode loaded = MessageMode.fromString(data.messageMode);
                    if (loaded != null) {
                        this.messageMode = loaded;
                    }
                }
                if (Autotip.DEBUG) {
                    Autotip.getInstance().getMessageUtil().log("Config loaded, messageMode: " + messageMode);
                }
            } catch (Exception e) {
                Autotip.getInstance().getMessageUtil().log("Failed to load config: " + e.getMessage());
            }
        }
    }

    private static class ConfigData {
        String messageMode;

        ConfigData(String messageMode) {
            this.messageMode = messageMode;
        }
    }
}
