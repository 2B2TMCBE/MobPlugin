package nukkitcoders.mobplugin.entities.monster.walking;

import cn.nukkit.Player;
import cn.nukkit.block.BlockLiquid;
import cn.nukkit.entity.Entity;
import cn.nukkit.entity.EntityCreature;
import cn.nukkit.entity.EntityExplosive;
import cn.nukkit.entity.data.IntEntityData;
import cn.nukkit.event.entity.EntityDamageByEntityEvent;
import cn.nukkit.event.entity.ExplosionPrimeEvent;
import cn.nukkit.item.Item;
import cn.nukkit.level.Explosion;
import cn.nukkit.level.Sound;
import cn.nukkit.level.format.FullChunk;
import cn.nukkit.level.particle.HugeExplodeSeedParticle;
import cn.nukkit.math.NukkitMath;
import cn.nukkit.math.Vector2;
import cn.nukkit.math.Vector3;
import cn.nukkit.nbt.tag.CompoundTag;
import nukkitcoders.mobplugin.RouteFinderThreadPool;
import nukkitcoders.mobplugin.entities.monster.WalkingMonster;
import nukkitcoders.mobplugin.route.WalkerRouteFinder;
import nukkitcoders.mobplugin.runnable.RouteFinderSearchTask;
import nukkitcoders.mobplugin.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public class Creeper extends WalkingMonster implements EntityExplosive {

    public static final int NETWORK_ID = 33;

    private int bombTime = 0;

    public Creeper(FullChunk chunk, CompoundTag nbt) {
        super(chunk, nbt);
        this.route = new WalkerRouteFinder(this);
    }

    @Override
    public int getNetworkId() {
        return NETWORK_ID;
    }

    @Override
    public float getWidth() {
        return 0.6f;
    }

    @Override
    public float getHeight() {
        return 1.7f;
    }

    @Override
    public double getSpeed() {
        return 0.9;
    }

    @Override
    public void initEntity() {
        super.initEntity();

        this.setMaxHealth(20);
    }

    public int getBombTime() {
        return this.bombTime;
    }

    @Override
    public void explode() {
        ExplosionPrimeEvent ev = new ExplosionPrimeEvent(this, 2.8);
        this.server.getPluginManager().callEvent(ev);

        if (!ev.isCancelled()) {
            Explosion explosion = new Explosion(this, (float) ev.getForce(), this);

            if (ev.isBlockBreaking()) {
                explosion.explodeA();
            }

            explosion.explodeB();
            this.level.addParticle(new HugeExplodeSeedParticle(this));
        }

        this.close();
    }

    @Override
    public boolean onUpdate(int currentTick) {
        if (this.server.getDifficulty() < 1) {
            this.close();
            return false;
        }

        if (!this.isAlive()) {
            if (++this.deadTicks >= 23) {
                this.close();
                return false;
            }
            return true;
        }

        int tickDiff = currentTick - this.lastUpdate;
        this.lastUpdate = currentTick;
        this.entityBaseTick(tickDiff);

        if (!this.isMovement()) {
            return true;
        }
        if (this.age % 10 == 0 && this.route!=null && !this.route.isSearching()) {
            RouteFinderThreadPool.executeRouteFinderThread(new RouteFinderSearchTask(this.route));
            if (this.route.hasNext()) {
                this.target = this.route.next();
            }
        }

        if (this.isKnockback()) {
            this.move(this.motionX * tickDiff, this.motionY, this.motionZ * tickDiff);
            this.motionY -= this.getGravity() * tickDiff;
            this.updateMovement();
            return true;
        }

        this.checkTarget();

        if (this.followTarget != null && !this.followTarget.closed && this.followTarget.isAlive() && this.target != null) {
            double x = this.target.x - this.x;
            double z = this.target.z - this.z;

            double diff = Math.abs(x) + Math.abs(z);
            double distance = followTarget.distance(this);
            if (distance <= 4.5) {
                if (followTarget instanceof EntityCreature) {
                    if (bombTime >= 0) {
                        this.level.addSound(this, Sound.RANDOM_FUSE);
                        this.setDataProperty(new IntEntityData(Entity.DATA_FUSE_LENGTH, bombTime));
                        this.setDataFlag(DATA_FLAGS, DATA_FLAG_IGNITED, true);
                    }
                    this.bombTime += tickDiff;
                    if (this.bombTime >= 64) {
                        this.explode();
                        return false;
                    }
                } else if (Math.pow(this.x - target.x, 2) + Math.pow(this.z - target.z, 2) <= 1) {
                    this.moveTime = 0;
                }
            } else {
                this.bombTime -= tickDiff;
                if (this.bombTime < 0) {
                    this.bombTime = 0;
                    this.setDataFlag(DATA_FLAGS, DATA_FLAG_IGNITED, false);
                }

                this.motionX = this.getSpeed() * 0.15 * (x / diff);
                this.motionZ = this.getSpeed() * 0.15 * (z / diff);
            }
            this.yaw = Math.toDegrees(-Math.atan2(x / diff, z / diff));
        }

        double dx = this.motionX * tickDiff;
        double dz = this.motionZ * tickDiff;
        boolean isJump = this.checkJump(dx, dz);
        if (this.stayTime > 0) {
            this.stayTime -= tickDiff;
            this.move(0, this.motionY * tickDiff, 0);
        } else {
            Vector2 be = new Vector2(this.x + dx, this.z + dz);
            this.move(dx, this.motionY * tickDiff, dz);
            Vector2 af = new Vector2(this.x, this.z);

            if ((be.x != af.x || be.y != af.y) && !isJump) {
                this.moveTime -= 90 * tickDiff;
            }
        }

        if (!isJump) {
            if (this.onGround) {
                this.motionY = 0;
            } else if (this.motionY > -this.getGravity() * 4) {
                if (!(this.level.getBlock(new Vector3(NukkitMath.floorDouble(this.x), (int) (this.y + 0.8), NukkitMath.floorDouble(this.z))) instanceof BlockLiquid)) {
                    this.motionY -= this.getGravity() * 1;
                }
            } else {
                this.motionY -= this.getGravity() * tickDiff;
            }
        }
        this.updateMovement();
        if (this.route != null) {
            if (this.route.hasCurrentNode() && this.route.hasArrivedNode(this)) {
                this.target = null;
                if (this.route.hasNext()) {
                    this.target = this.route.next();
                }
            }
        }
        return true;
    }

    public void attackEntity(Entity player) {
    }

    @Override
    public Item[] getDrops() {
        List<Item> drops = new ArrayList<>();

        if (this.hasCustomName()) {
            drops.add(Item.get(Item.NAME_TAG, 0, 1));
        }

        if (this.lastDamageCause instanceof EntityDamageByEntityEvent && !this.isBaby()) {
            for (int i = 0; i < Utils.rand(0, 2); i++) {
                drops.add(Item.get(Item.GUNPOWDER, 0, 1));
            }
        }

        return drops.toArray(new Item[0]);
    }

    @Override
    public int getKillExperience() {
        return this.isBaby() ? 0 : 5;
    }

    public int getMaxFallHeight() {
        return this.followTarget == null ? 3 : 3 + (int) (this.getHealth() - 1.0F);
    }

    @Override
    public boolean onInteract(Player player, Item item) {
        super.onInteract(player, item);
        if (item.getId() == Item.FLINT_AND_STEEL) {
            this.level.addSound(this, Sound.FIRE_IGNITE);
            this.explode();
            return true;
        }
        return false;
    }
}
