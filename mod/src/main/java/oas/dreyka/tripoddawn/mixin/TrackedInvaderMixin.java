package oas.dreyka.tripoddawn.mixin;

import oas.dreyka.tripoddawn.entity.MachineEntity;
import oas.dreyka.tripoddawn.entity.MartianEntity;

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
 * Sends the invasion to a player who is much too far away to be told about an ordinary mob.
 *
 * <p>The vanilla tracker stops at the shorter of the entity type's own range and the player's view
 * distance, and it refuses outright to send an entity whose chunk that player has not been given.
 * Both rules are right for a cow and wrong for the one thing in this mod a player is meant to see
 * before anything else: a machine is a silhouette on the horizon first and an opponent second, and
 * at twelve chunks of view distance the horizon is a hundred and ninety blocks away, which a
 * hundred block machine crosses in under a minute.
 *
 * <p>It matters more with a mod that draws terrain far past what the server sends, Voxy and the
 * others like it. There the player sees ground for a kilometre and the creatures walking on it were
 * the only thing missing, so a valley that should have had an invasion crossing it read as empty.
 *
 * <p>Only what the invasion brought, only the position packets the tracker already builds, and only
 * out to a fixed distance: everything else in the world keeps the vanilla rule.
 */
@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public abstract class TrackedInvaderMixin {

    /**
     * How far a machine is sent, in blocks.
     *
     * <p>Two kilometres, which covers what the far terrain mods draw. Past it the model is under a
     * pixel and the cost of telling every player about every machine stops being worth the dot.
     */
    private static final double MACHINE_SIGHT = 2048.0;

    /**
     * How far a martian is sent.
     *
     * <p>Shorter, and for a different reason than distance. A night stands dozens of them where it
     * stands one machine, and a foot soldier is a couple of pixels at this range: what the reach buys
     * is a horizon that still moves, not a creature anyone picks out.
     */
    private static final double MARTIAN_SIGHT = 512.0;

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
    private void tripoddawn$reachFurtherForInvaders(ServerPlayer player, CallbackInfo info) {
        double sight = sight(this.entity);
        if (sight <= 0.0 || player == this.entity) {
            return;
        }
        info.cancel();

        // Measured flat, like the vanilla test it replaces: a player in a mine under a machine is
        // still standing where it can walk to them.
        double dx = player.getX() - this.entity.getX();
        double dz = player.getZ() - this.entity.getZ();
        boolean within = dx * dx + dz * dz <= sight * sight && this.entity.broadcastToPlayer(player);

        if (within) {
            if (this.seenBy.add(player.connection)) {
                this.serverEntity.addPairing(player);
            }
        } else if (this.seenBy.remove(player.connection)) {
            this.serverEntity.removePairing(player);
        }
    }

    /** How far this one carries, or zero for everything that keeps the vanilla rule. */
    private static double sight(Entity entity) {
        if (entity instanceof MachineEntity) {
            return MACHINE_SIGHT;
        }
        return entity instanceof MartianEntity ? MARTIAN_SIGHT : 0.0;
    }
}
