package oas.dreyka.tripoddawn.invasion;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * What the invasion remembers between two sessions, stored beside the world.
 *
 * <p>Five things, and they all exist so that something happens once instead of every tick: how many
 * days the invasion has been shifted by a command, which night has already been run, whether the two
 * one-off events have fired, and when the opening barrage runs out.
 */
public class InvasionState extends SavedData {

    public static final int TICKS_PER_DAY = 24000;

    public static final Codec<InvasionState> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.optionalFieldOf("day_shift", 0).forGetter(state -> state.dayShift),
            Codec.LONG.optionalFieldOf("last_night", -1L).forGetter(state -> state.lastNight),
            Codec.BOOL.optionalFieldOf("omen_done", false).forGetter(state -> state.omenDone),
            Codec.BOOL.optionalFieldOf("emperor_done", false).forGetter(state -> state.emperorDone),
            Codec.LONG.optionalFieldOf("storm_until", 0L).forGetter(state -> state.stormUntil)
    ).apply(instance, InvasionState::new));

    public static final SavedDataType<InvasionState> TYPE = new SavedDataType<>(
            "tripoddawn_invasion", InvasionState::new, CODEC, DataFixTypes.LEVEL);

    private int dayShift;
    private long lastNight;
    private boolean omenDone;
    private boolean emperorDone;
    private long stormUntil;

    public InvasionState() {
        this(0, -1L, false, false, 0L);
    }

    private InvasionState(int dayShift, long lastNight, boolean omenDone, boolean emperorDone,
                          long stormUntil) {
        this.dayShift = dayShift;
        this.lastNight = lastNight;
        this.omenDone = omenDone;
        this.emperorDone = emperorDone;
        this.stormUntil = stormUntil;
    }

    public static InvasionState of(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }

    /**
     * The day the invasion thinks it is on, which is the world's own day plus whatever a command
     * added to it. Read off the day time rather than the game time so that a server owner moving the
     * clock moves the invasion with it.
     */
    public long day(ServerLevel level) {
        return level.getDayTime() / TICKS_PER_DAY + this.dayShift;
    }

    public InvasionTier tier(ServerLevel level) {
        return InvasionTier.forDay(day(level));
    }

    /** Jumps the invasion to a given day without touching the world clock. */
    public void setDay(ServerLevel level, long wanted) {
        this.dayShift = (int) (wanted - level.getDayTime() / TICKS_PER_DAY);
        this.lastNight = -1L;
        setDirty();
    }

    /** Puts the invasion back where the world's own clock leaves it, one-off events included. */
    public void reset() {
        this.dayShift = 0;
        this.lastNight = -1L;
        this.omenDone = false;
        this.emperorDone = false;
        this.stormUntil = 0L;
        setDirty();
    }

    /**
     * Opens the window the sky is struck through. Held on the game time rather than the day time,
     * because a command moving the clock during a barrage would otherwise end it or make it eternal.
     */
    public void openStorm(ServerLevel level, int ticks) {
        this.stormUntil = level.getGameTime() + ticks;
        setDirty();
    }

    public boolean storming(ServerLevel level) {
        return level.getGameTime() < this.stormUntil;
    }

    /** True once per night, for the first caller of that night. */
    public boolean claimNight(long day) {
        if (this.lastNight == day) {
            return false;
        }
        this.lastNight = day;
        setDirty();
        return true;
    }

    /** Lets the next night run again even though it already has: what the test command needs. */
    public void forgetNight() {
        this.lastNight = -1L;
        setDirty();
    }

    public boolean claimOmen() {
        if (this.omenDone) {
            return false;
        }
        this.omenDone = true;
        setDirty();
        return true;
    }

    public boolean claimEmperor() {
        if (this.emperorDone) {
            return false;
        }
        this.emperorDone = true;
        setDirty();
        return true;
    }
}
