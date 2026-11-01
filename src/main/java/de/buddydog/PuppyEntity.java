package de.buddydog;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.wolf.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Der kleine Hund. Basiert auf dem Wolf (damit Modell, Sounds, Zaehmen, Sitzen
 * und Besitzer schon fertig vorhanden sind), kaempft aber nie, sondern versteckt sich.
 */
public class PuppyEntity extends Wolf {

    private static final EntityDataAccessor<Boolean> DATA_LYING =
            SynchedEntityData.defineId(PuppyEntity.class, EntityDataSerializers.BOOLEAN);

    public PuppyEntity(EntityType<? extends Wolf> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Wolf.createAttributes()
                .add(Attributes.SCALE, 0.6)          // klein = Welpe
                .add(Attributes.MAX_HEALTH, 20.0);
    }

    @Override
    protected void registerGoals() {
        // Bewusst KEIN super.registerGoals(): der Wolf wuerde angreifen. Unser Hund nicht.
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(3, new HideBehindOwnerGoal(this, 1.3, 14.0));
        this.goalSelector.addGoal(4, new FollowOwnerGoal(this, 1.1, 6.0F, 2.0F));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_LYING, false);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("Lying", isLying());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(DATA_LYING, input.getBooleanOr("Lying", false));
    }

    @Override
    public void tick() {
        super.tick();
        // Wenn der Spieler den Hund per Rechtsklick wieder aufstehen laesst, ist auch "Platz" vorbei.
        if (!level().isClientSide() && isLying() && !isOrderedToSit()) {
            this.entityData.set(DATA_LYING, false);
        }
    }

    public boolean isLying() {
        return this.entityData.get(DATA_LYING);
    }

    /** "Sitz" */
    public void commandSit() {
        this.entityData.set(DATA_LYING, false);
        setOrderedToSit(true);
        setInSittingPose(true);
        getNavigation().stop();
    }

    /** "Platz" */
    public void commandDown() {
        setOrderedToSit(true);
        setInSittingPose(true);
        this.entityData.set(DATA_LYING, true);
        getNavigation().stop();
    }

    /** "Komm" / "Auf" */
    public void commandStand() {
        this.entityData.set(DATA_LYING, false);
        setOrderedToSit(false);
        setInSittingPose(false);
    }
}
