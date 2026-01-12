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

package club.sk1er.mods.autotip.command;

import club.sk1er.mods.autotip.Autotip;
import club.sk1er.mods.autotip.util.HypixelUtil;
import club.sk1er.mods.autotip.util.MessageUtil;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

public class CommandManager {

    private final MessageUtil messageUtil;

    public CommandManager() {
        this.messageUtil = Autotip.getInstance().getMessageUtil();
        registerCommands();
    }

    private void registerCommands() {
        // Fabric v2 proper registration
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {

            // /autotip login
            dispatcher.register(ClientCommandManager.literal("autotip")
                    .then(ClientCommandManager.literal("status") // TODO: Better command feedback, maybe interactive click
                            .executes(context -> {
                                boolean onHypixel = HypixelUtil.isOnHypixel();
                                messageUtil.send("§aAutotip status:");
                                messageUtil.send("§6 - On Hypixel: " + onHypixel);
                                return 1;
                            }))
            );

            messageUtil.send("&aAutotip commands registered: /autotip status");
        });
    }
}
