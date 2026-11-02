package dev.warbot;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ThreadLocalRandom;

public class WarBotMod implements ModInitializer {
    private static final int MAX_BOTS_PER_COMMAND = 8;
    private static final String USERNAME_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789_";

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("warbot")
                .then(Commands.literal("spawn")
                    .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                    .then(Commands.argument("number", IntegerArgumentType.integer(1, MAX_BOTS_PER_COMMAND))
                        .executes(context -> spawnBots(
                            context.getSource().getPlayerOrException(),
                            IntegerArgumentType.getInteger(context, "number")
                        ))))));
    }

    private static int spawnBots(ServerPlayer target, int count) {
        ServerLevel level = target.level();
        MinecraftServer server = level.getServer();
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int spawned = 0;

        for (int index = 0; index < count; index++) {
            String name = nextBotName(server, random);
            double angle = random.nextDouble(0, Math.PI * 2);
            double distance = random.nextDouble(3, 5);
            double x = target.getX() + Math.cos(angle) * distance;
            double z = target.getZ() + Math.sin(angle) * distance;
            float yaw = random.nextFloat() * 360;

            if (WarBotPlayer.spawn(server, level, name, x, target.getY(), z, yaw)) {
                spawned++;
            }
        }

        target.sendSystemMessage(Component.literal("Spawned " + spawned + " practice bot(s)."));
        return spawned;
    }

    private static String nextBotName(MinecraftServer server, ThreadLocalRandom random) {
        String name;
        do {
            StringBuilder generatedName = new StringBuilder(16);
            for (int index = 0; index < 16; index++) {
                generatedName.append(USERNAME_CHARACTERS.charAt(random.nextInt(USERNAME_CHARACTERS.length())));
            }
            name = generatedName.toString();
        } while (isNameOnline(server, name));
        return name;
    }

    private static boolean isNameOnline(MinecraftServer server, String name) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (player.getGameProfile().name().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }
}