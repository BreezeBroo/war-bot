package dev.warbot;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.scores.PlayerTeam;

import java.util.Set;

public final class WarBotPlayer extends ServerPlayer {
    private static final double TARGET_RANGE_SQUARED = 24 * 24;
    private static final String BOT_TEAM_NAME = "warbot_practice";
    private int deadTicks;

    private WarBotPlayer(MinecraftServer server, ServerLevel level, GameProfile profile) {
        super(server, level, profile, ClientInformation.createDefault());
        addTag("warbot_practice");
        equip();
    }

    public static boolean spawn(
        MinecraftServer server,
        ServerLevel level,
        String name,
        double x,
        double y,
        double z,
        float yaw
    ) {
        GameProfile profile = new GameProfile(UUIDUtil.createOfflinePlayerUUID(name), name);
        WarBotPlayer bot = new WarBotPlayer(server, level, profile);
        bot.setHealth(bot.getMaxHealth());
        server.getPlayerList().placeNewPlayer(
            new WarBotConnection(PacketFlow.SERVERBOUND),
            bot,
            new CommonListenerCookie(profile, 0, bot.clientInformation(), false)
        );
        PlayerTeam team = server.getScoreboard().getPlayerTeam(BOT_TEAM_NAME);
        if (team == null) {
            team = server.getScoreboard().addPlayerTeam(BOT_TEAM_NAME);
        }
        server.getScoreboard().addPlayerToTeam(name, team);
        bot.teleportTo(level, x, y, z, Set.of(), yaw, 0, true);
        return true;
    }

    @Override
    public void tick() {
        super.tick();
        doTick();

        if (!isAlive()) {
            if (++deadTicks > 20) {
                level().getServer().getPlayerList().remove(this);
            }
            return;
        }

        tickSparring();
    }

    private void tickSparring() {
        Player target = findTarget();
        if (target == null) {
            zza = 0;
            xxa = 0;
            setSprinting(false);
            return;
        }

        double dx = target.getX() - getX();
        double dy = target.getY() + target.getEyeHeight() * 0.9 - getEyeY();
        double dz = target.getZ() - getZ();
        double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDistance));
        setYRot(yaw);
        setXRot(pitch);
        yHeadRot = yaw;
        yBodyRot = yaw;

        double distanceSquared = distanceToSqr(target);
        zza = distanceSquared > 9 ? 1 : 0;
        setSprinting(distanceSquared > 16);
        xxa = distanceSquared > 4 && distanceSquared < 64
            ? (level().getServer().getTickCount() / 20 + getId()) % 2 == 0 ? 0.3F : -0.3F
            : 0;

        if (target.getY() > getY() + 1 && onGround()
            && level().getServer().getTickCount() % 20 == Math.floorMod(getId(), 20)) {
            jumpFromGround();
        }

        if (distanceSquared <= 3.2 * 3.2 && hasLineOfSight(target)
            && getAttackStrengthScale(0.5F) >= 0.9F) {
            attack(target);
            swing(InteractionHand.MAIN_HAND);
            resetAttackStrengthTicker();
        }
    }

    private Player findTarget() {
        Player nearest = null;
        double nearestDistance = TARGET_RANGE_SQUARED;
        for (ServerPlayer player : level().getServer().getPlayerList().getPlayers()) {
            if (player == this || player instanceof WarBotPlayer || !player.isAlive()
                || player.isSpectator() || player.level() != level()) {
                continue;
            }

            double distance = distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private void equip() {
        setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.IRON_SWORD));
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.IRON_HELMET));
        setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.IRON_CHESTPLATE));
        setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.IRON_LEGGINGS));
        setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.IRON_BOOTS));
    }
}