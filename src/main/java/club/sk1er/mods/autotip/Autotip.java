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

package club.sk1er.mods.autotip;

import club.sk1er.mods.autotip.api.AutotipHttpClient;
import club.sk1er.mods.autotip.auth.AuthManager;
import club.sk1er.mods.autotip.command.CommandManager;
import club.sk1er.mods.autotip.util.HypixelUtil;
import club.sk1er.mods.autotip.util.MessageUtil;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

public class Autotip implements ClientModInitializer {
    private MessageUtil messageUtil;
    private AuthManager authManager;
    private AutotipHttpClient autotipHttpClient;
    private static Autotip instance;
    private CommandManager commandManager;
    public static final String VERSION = "3.3";

    @Override
    public void onInitializeClient() {
        instance = this;
        messageUtil = new MessageUtil();
        authManager = new AuthManager();
        autotipHttpClient = new AutotipHttpClient();
        commandManager = new CommandManager();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            HypixelUtil.onServerJoin();
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            HypixelUtil.onServerDisconnect();
        });
    }

    public static Autotip getInstance() {
        return instance;
    }

    public MessageUtil getMessageUtil() {
        return messageUtil;
    }

    public AutotipHttpClient getAutotipHttpClient() {
        return autotipHttpClient;
    }

    public AuthManager getAuthManager() {
        return authManager;
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
