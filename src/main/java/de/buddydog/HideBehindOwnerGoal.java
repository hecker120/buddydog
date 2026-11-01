package de.buddydog;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Sind Monster in der Naehe, versteckt sich der Hund auf der vom Monster
 * abgewandten Seite seines Besitzers.
 */
public class HideBehindOwnerGoal extends Goal {

    private final PuppyEntity puppy;
    private final double speed;
    private final double scanRadius;

    private LivingEntity owner;
    private Mob threat;
    private int recalcTimer;

    public HideBehindOwnerGoal(PuppyEntity puppy, double speed, double scanRadius) {
        this.puppy = puppy;
        this.speed = speed;
        this.scanRadius = scanRadius;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (puppy.isOrderedToSit() || puppy.isInSittingPose()) return false;
        LivingEntity o = puppy.getOwner();
        if (o == null || !o.isAlive() || puppy.distanceToSqr(o) > 24 * 24) return false;
        Mob nearest = findNearestThreat(o);
        if (nearest == null) return false;
        this.owner = o;
        this.threat = nearest;
        return true;
    }

    @Override
    public boolean canContinueToUse() {
        if (puppy.isOrderedToSit() || owner == null || !owner.isAlive()) return false;
        threat = findNearestThreat(owner);
        return threat != null;
    }

    @Override
    public void start() {
        recalcTimer = 0;
    }

    @Override
    public void stop() {
        puppy.getNavigation().stop();
        threat = null;
    }

    @Override
    public void tick() {
        if (threat == null || owner == null) return;

        puppy.getLookControl().setLookAt(threat, 30.0F, 30.0F);

        if (--recalcTimer > 0) return;
        recalcTimer = adjustedTickDelay(8);

        // Punkt hinter dem Besitzer, gesehen vom Monster aus
        Vec3 away = owner.position().subtract(threat.position());
        Vec3 dir = new Vec3(away.x, 0, away.z);
        if (dir.lengthSqr() < 1.0E-4) {
            dir = new Vec3(1, 0, 0);
        }
        dir = dir.normalize();
        Vec3 hideSpot = owner.position().add(dir.scale(1.6));

        if (puppy.position().distanceToSqr(hideSpot) > 1.0) {
            puppy.getNavigation().moveTo(hideSpot.x, hideSpot.y, hideSpot.z, speed);
        } else {
            puppy.getNavigation().stop();
        }
    }

    private Mob findNearestThreat(LivingEntity around) {
        AABB box = around.getBoundingBox().inflate(scanRadius);
        List<Mob> mobs = puppy.level().getEntitiesOfClass(
                Mob.class, box, m -> m instanceof Enemy && m.isAlive() && m != puppy);
        return mobs.stream()
                .min(Comparator.comparingDouble(m -> m.distanceToSqr(around)))
                .orElse(null);
    }
}
