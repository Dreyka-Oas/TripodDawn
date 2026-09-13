package oas.dreyka.tripoddawn.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/**
 * One particle for all four of the mod's effects, told apart by the settings it is built with.
 *
 * <p>The animation and the fade come from {@link SimpleAnimatedParticle}, which walks the sprite
 * list across the particle's life and thins it out over the second half. What is left to decide is
 * how long it lives, how big it starts, whether it falls and whether it lights itself.
 */
public class TripodDawnParticle extends SimpleAnimatedParticle {

    private final boolean selfLit;

    protected TripodDawnParticle(ClientLevel level, double x, double y, double z,
                                 double xd, double yd, double zd, SpriteSet sprites, Settings settings) {
        super(level, x, y, z, sprites, settings.gravity());
        this.lifetime = settings.minLife() + this.random.nextInt(settings.lifeSpread());
        this.quadSize = settings.size() * (0.75f + this.random.nextFloat() * 0.5f);
        this.friction = settings.friction();
        this.hasPhysics = settings.physics();
        this.selfLit = settings.selfLit();
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        setSpriteFromAge(sprites);
    }

    @Override
    public int getLightColor(float partialTick) {
        // A beam has to read at the bottom of a cave at midnight, so it carries its own light rather
        // than taking the block's.
        return this.selfLit ? 0x00F000F0 : super.getLightColor(partialTick);
    }

    /**
     * @param gravity     downward pull, zero for anything that hangs in the air
     * @param friction    per-tick speed kept, one for no drag
     * @param size        the quad's base width, jittered per particle
     * @param minLife     shortest life in ticks
     * @param lifeSpread  how many ticks of spread sit on top of it
     * @param physics     whether it collides with blocks
     * @param selfLit     whether it renders at full brightness
     */
    public record Settings(float gravity, float friction, float size, int minLife, int lifeSpread,
                           boolean physics, boolean selfLit) {
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;
        private final Settings settings;

        public Provider(SpriteSet sprites, Settings settings) {
            this.sprites = sprites;
            this.settings = settings;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double xd, double yd, double zd, RandomSource random) {
            return new TripodDawnParticle(level, x, y, z, xd, yd, zd, this.sprites, this.settings);
        }
    }
}
