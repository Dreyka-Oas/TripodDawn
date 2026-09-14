package oas.dreyka.tripoddawn.command;

import oas.dreyka.tripoddawn.entity.MachineEntity;
import oas.dreyka.tripoddawn.entity.TripodDawnEntities;
import oas.dreyka.tripoddawn.entity.TripodEntity;
import oas.dreyka.tripoddawn.entity.TripodVariant;
import oas.dreyka.tripoddawn.invasion.InvasionSpawner;
import oas.dreyka.tripoddawn.invasion.InvasionState;
import oas.dreyka.tripoddawn.invasion.InvasionNight;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The operator side of the invasion.
 *
 * <p>It exists to be able to look at day thirty without playing thirty days, so every subcommand is
 * a shortcut through the calendar rather than a cheat inside it. This is also the one place in the
 * mod that writes to the chat, and only to whoever typed the command.
 */
public final class TripodDawnCommands {
    private TripodDawnCommands() {
    }

    /** What `spawn` accepts. Literals rather than an entity argument, so the game completes them. */
    private static final Map<String, EntityType<? extends Mob>> SUMMONABLE = Map.of(
            "martian", TripodDawnEntities.MARTIAN,
            "tripod", TripodDawnEntities.TRIPOD,
            "harvester", TripodDawnEntities.HARVESTER,
            "uberpod", TripodDawnEntities.UBERPOD,
            "emperorpod", TripodDawnEntities.EMPERORPOD,
            "titan", TripodDawnEntities.TITAN);

    /**
     * The two builds that are not the line machine, since `tripod` on its own takes the one the
     * calendar is currently sending and there is otherwise no way to ask for a given build.
     */
    private static final Map<String, TripodVariant> BUILDS = Map.of(
            "scout", TripodVariant.SCOUT,
            "heavy", TripodVariant.HEAVY);

    private static final int SPAWN_MIN = 32;
    private static final int SPAWN_MAX = 64;

    /** One of each, in the order they turn up over a campaign. */
    private static final List<Lineup> LINEUP = List.of(
            new Lineup(TripodDawnEntities.MARTIAN, null),
            new Lineup(TripodDawnEntities.TRIPOD, TripodVariant.SCOUT),
            new Lineup(TripodDawnEntities.TRIPOD, TripodVariant.LINE),
            new Lineup(TripodDawnEntities.TRIPOD, TripodVariant.HEAVY),
            new Lineup(TripodDawnEntities.HARVESTER, null),
            new Lineup(TripodDawnEntities.UBERPOD, null),
            new Lineup(TripodDawnEntities.EMPERORPOD, null),
            new Lineup(TripodDawnEntities.TITAN, null));

    /** Far enough back that the tallest of them fits on the screen, spaced so none overlaps. */
    private static final double LINEUP_AWAY = 90.0;
    private static final double LINEUP_GAP = 34.0;

    private record Lineup(EntityType<? extends Mob> type, TripodVariant build) {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> build(dispatcher));
    }

    private static void build(CommandDispatcher<CommandSourceStack> dispatcher) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("tripoddawn")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS));

        root.then(Commands.literal("day")
                .executes(TripodDawnCommands::readDay)
                .then(Commands.argument("number", IntegerArgumentType.integer(0))
                        .executes(context -> setDay(context, IntegerArgumentType.getInteger(context, "number")))));

        root.then(Commands.literal("wave").executes(TripodDawnCommands::wave));
        root.then(Commands.literal("reset").executes(TripodDawnCommands::reset));
        root.then(Commands.literal("lineup").executes(TripodDawnCommands::lineup));

        LiteralArgumentBuilder<CommandSourceStack> spawn = Commands.literal("spawn");
        for (Map.Entry<String, EntityType<? extends Mob>> entry : SUMMONABLE.entrySet()) {
            spawn.then(Commands.literal(entry.getKey())
                    .executes(context -> summon(context, entry.getValue(), null)));
        }
        for (Map.Entry<String, TripodVariant> entry : BUILDS.entrySet()) {
            spawn.then(Commands.literal(entry.getKey())
                    .executes(context -> summon(context, TripodDawnEntities.TRIPOD, entry.getValue())));
        }
        root.then(spawn);

        dispatcher.register(root);
    }

    private static int readDay(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        InvasionState state = InvasionState.of(level);
        long day = state.day(level);
        report(context, "commands.tripoddawn.day.read", day);
        return (int) day;
    }

    /**
     * Moves the invasion without moving the world clock, and arms the night again so that a jump
     * made after dark lands within the second rather than waiting for the next dusk.
     */
    private static int setDay(CommandContext<CommandSourceStack> context, int day) {
        ServerLevel level = context.getSource().getLevel();
        InvasionState state = InvasionState.of(level);
        state.setDay(level, day);
        report(context, "commands.tripoddawn.day.set", day);
        return day;
    }

    /** Tonight's arrivals, now, whatever the sky says. */
    private static int wave(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        InvasionState state = InvasionState.of(level);
        long day = state.day(level);
        state.forgetNight();
        int spawned = InvasionSpawner.runNight(level, state, InvasionNight.of(day));
        context.getSource().sendSuccess(
                () -> Component.translatable("commands.tripoddawn.wave", spawned), true);
        return spawned;
    }

    private static int reset(CommandContext<CommandSourceStack> context) {
        ServerLevel level = context.getSource().getLevel();
        InvasionState state = InvasionState.of(level);
        state.reset();
        long day = state.day(level);
        report(context, "commands.tripoddawn.reset", day);
        return (int) day;
    }

    /**
     * One of every machine there is, side by side and holding still.
     *
     * <p>For looking at rather than for fighting: each one keeps its idle animation and its hit
     * boxes, and none of them walks off or shoots back.
     */
    private static int lineup(CommandContext<CommandSourceStack> context) {
        CommandSourceStack source = context.getSource();
        ServerLevel level = source.getLevel();
        Vec3 from = source.getPosition();
        float yaw = source.getRotation().y;
        Vec3 ahead = Vec3.directionFromRotation(0.0f, yaw);
        Vec3 across = Vec3.directionFromRotation(0.0f, yaw + 90.0f);
        float facing = yaw + 180.0f;
        int standing = 0;
        for (int i = 0; i < LINEUP.size(); i++) {
            Vec3 spot = from.add(ahead.scale(LINEUP_AWAY))
                    .add(across.scale((i - (LINEUP.size() - 1) * 0.5) * LINEUP_GAP));
            BlockPos ground = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    BlockPos.containing(spot));
            Lineup piece = LINEUP.get(i);
            Mob mob = piece.type().spawn(level, ground, EntitySpawnReason.COMMAND);
            if (mob == null) {
                continue;
            }
            if (piece.build() != null && mob instanceof TripodEntity tripod) {
                tripod.setVariant(piece.build());
            }
            if (mob instanceof MachineEntity machine) {
                machine.skipEmerge();
            }
            // Turned by hand as well as placed, since the columns of hit boxes stand where the body
            // is pointed and a machine dropped in keeps whatever way the spawn left it looking.
            mob.snapTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, facing, 0.0f);
            mob.setYBodyRot(facing);
            mob.setYHeadRot(facing);
            mob.setNoAi(true);
            mob.setPersistenceRequired();
            standing++;
        }
        int count = standing;
        source.sendSuccess(() -> Component.translatable("commands.tripoddawn.lineup", count), true);
        return standing;
    }

    private static int summon(CommandContext<CommandSourceStack> context, EntityType<? extends Mob> type,
                              TripodVariant build) {
        CommandSourceStack source = context.getSource();
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            source.sendFailure(Component.translatable("commands.tripoddawn.spawn.no_player"));
            return 0;
        }
        Mob mob = InvasionSpawner.place(source.getLevel(), type, player, SPAWN_MIN, SPAWN_MAX,
                MachineEntity.class.isAssignableFrom(type.getBaseClass()));
        if (mob == null) {
            source.sendFailure(Component.translatable("commands.tripoddawn.spawn.no_room"));
            return 0;
        }
        if (build != null && mob instanceof TripodEntity tripod) {
            tripod.setVariant(build);
        }
        // Half a minute of rise is the scene a night gives; someone typing this wants the machine.
        if (mob instanceof MachineEntity machine) {
            machine.skipEmerge();
        }
        source.sendSuccess(() -> Component.translatable("commands.tripoddawn.spawn",
                mob.getDisplayName(), mob.getBlockX(), mob.getBlockZ()), true);
        return 1;
    }

    /**
     * Says where the invasion is. Past the last written rung the name alone stops meaning anything,
     * since every night from day thirty on is called the emperor, so the step is spelled out beside
     * it.
     */
    private static void report(CommandContext<CommandSourceStack> context, String key, long day) {
        InvasionNight night = InvasionNight.of(day);
        Component name = Component.translatable(
                "tripoddawn.tier." + night.tier().name().toLowerCase(Locale.ROOT));
        Component stage = night.step() == 0 ? name
                : Component.translatable("tripoddawn.tier.beyond", name, night.step());
        context.getSource().sendSuccess(() -> Component.translatable(key, day, stage), true);
    }
}
