package de.buddydog;

import java.util.List;
import java.util.function.Consumer;

import com.mojang.brigadier.CommandDispatcher;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class BuddyDogMod implements ModInitializer {

    public static final String MOD_ID = "buddydog";

    public static final ResourceKey<EntityType<?>> PUPPY_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "welpe"));

    public static final EntityType<PuppyEntity> PUPPY = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            PUPPY_KEY,
            EntityType.Builder.of(PuppyEntity::new, MobCategory.CREATURE)
                    .sized(0.6F, 0.85F)
                    .clientTrackingRange(10)
                    .build(PUPPY_KEY));

    @Override
    public void onInitialize() {
        FabricDefaultAttributeRegistry.register(PUPPY, PuppyEntity.createAttributes());
        CommandRegistrationCallback.EVENT.register(BuddyDogMod::registerCommands);
    }

    // ------------------------------------------------------------------ Befehle

    private static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher,
                                         CommandBuildContext context,
                                         Commands.CommandSelection selection) {

        dispatcher.register(Commands.literal("welpe").executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            ServerLevel level = player.level();
            PuppyEntity puppy = PUPPY.create(level, EntitySpawnReason.COMMAND);
            if (puppy == null) return 0;
            puppy.snapTo(player.getX(), player.getY(), player.getZ(), player.getYRot(), 0.0F);
            puppy.tame(player);
            level.addFreshEntity(puppy);
            ctx.getSource().sendSuccess(() -> Component.literal("Dein Welpe ist da!"), false);
            return 1;
        }));

        registerCommand(dispatcher, "sitz", PuppyEntity::commandSit, "Sitz!");
        registerCommand(dispatcher, "platz", PuppyEntity::commandDown, "Platz!");
        registerCommand(dispatcher, "komm", PuppyEntity::commandStand, "Komm!");
    }

    private static void registerCommand(CommandDispatcher<CommandSourceStack> dispatcher, String name,
                                        Consumer<PuppyEntity> action, String message) {
        dispatcher.register(Commands.literal(name).executes(ctx -> {
            ServerPlayer player = ctx.getSource().getPlayerOrException();
            int n = commandPuppiesOf(player, action);
            if (n == 0) {
                ctx.getSource().sendFailure(Component.literal("Kein Welpe in der Naehe. Mit /welpe holst du einen."));
            } else {
                ctx.getSource().sendSuccess(() -> Component.literal(message), false);
            }
            return n;
        }));
    }

    // ------------------------------------------------- gemeinsam genutzt (auch von der Sprachsteuerung)

    /** Alle Welpen des Spielers im Umkreis von 32 Bloecken. */
    public static List<PuppyEntity> puppiesOf(ServerPlayer player) {
        return player.level().getEntitiesOfClass(
                PuppyEntity.class,
                player.getBoundingBox().inflate(32.0),
                p -> p.isAlive() && p.isOwnedBy(player));
    }

    /** Fuehrt eine Aktion bei allen eigenen Welpen aus. Nur auf dem Server-Thread aufrufen! */
    public static int commandPuppiesOf(ServerPlayer player, Consumer<PuppyEntity> action) {
        List<PuppyEntity> list = puppiesOf(player);
        list.forEach(action);
        return list.size();
    }
}
