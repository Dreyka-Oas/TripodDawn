package oas.dreyka.tripoddawn.mixin;

import oas.dreyka.tripoddawn.entity.MachineEntity;

import net.minecraft.server.level.ServerEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerPlayerConnection;
import net.minecraft.world.entity.Entity;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Sends the machines to a player who is much too far away to be told about an ordinary mob.
 *
 * <p>The vanilla tracker stops at the shorter of the entity type's own range and the player's view
 * distance, and it refuses outright to send an entity whose chunk that player has not been given.
 * Both rules are right for a cow and wrong for the one thing in this mod a player is meant to see
 * before anything else: a machine is a silhouette on the horizon first and an opponent second, and
 * at twelve chunks of view distance the horizon is a hundred and ninety blocks away, which a
 * hundred block machine crosses in under a minute.
 *
 * <p>It matters more with a mod that draws terrain far past what the server sends, Voxy and the
 * others like it. There the player sees ground for a kilometre and the machines walking on it were
 * the only thing missing, so a valley that should have had one crossing it read as empty.
 *
 * <p>Only the machines, only the position packets the tracker already builds, and only out to a
 * fixed distance: everything else in the world keeps the vanilla rule.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedMachineMixin {

    /**
     * How far a machine is sent, in blocks.
     *
     * <p>Sixty-four chunks, which is what the far terrain mods are usually set to. Past it the model
     * is a handful of pixels and the cost of telling every player about every machine stops being
     * worth the dot they would see.
     */
    private static final double SIGHT = 1024.0;

    @Shadow
    @Final
    private Entity entity;

    @Shadow
    @Final
    private ServerEntity serverEntity;

    @Shadow
    @Final
    private Set<ServerPlayerConnection> seenBy;

    @Inject(method = "updatePlayer", at = @At("HEAD"), cancellable = true)
    private void tripoddawn$reachFurtherForMachines(ServerPlayer player, CallbackInfo info) {
        if (!(this.entity instanceof MachineEntity) || player == this.entity) {
            return;
        }
        info.cancel();

        // Measured flat, like the vanilla test it replaces: a player in a mine under a machine is
        // still standing where it can walk to them.
        double dx = player.getX() - this.entity.getX();
        double dz = player.getZ() - this.entity.getZ();
        boolean within = dx * dx + dz * dz <= SIGHT * SIGHT && this.entity.broadcastToPlayer(player);

        if (within) {
            if (this.seenBy.add(player.connection)) {
                this.serverEntity.addPairing(player);
            }
        } else if (this.seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
        }
    }
}
