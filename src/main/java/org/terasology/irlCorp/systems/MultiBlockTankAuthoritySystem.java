// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.math.Region3i;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.regions.ActAsBlockComponent;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.health.logic.HealthComponent;
import org.terasology.irlCorp.components.MultiBlockActivatorComponent;
import org.terasology.irlCorp.components.MultiBlockFluidTankComponent;
import org.terasology.irlCorp.components.MultiBlockTankWallComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormRecipeRegistry;
import org.terasology.multiBlock.recipe.MultiBlockFormItemRecipe;
import org.terasology.multiBlock.recipe.UniformMultiBlockFormItemRecipe;

import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MultiBlockTankAuthoritySystem extends BaseComponentSystem {
    @In
    MultiBlockFormRecipeRegistry multiBlockFormRecipeRegistry;
    @In
    BlockEntityRegistry blockEntityRegistry;

    @Override
    public void initialise() {
        super.initialise();

        MultiBlockFormItemRecipe recipe = new UniformMultiBlockFormItemRecipe(
                x -> x.hasComponent(MultiBlockActivatorComponent.class),
                x -> true,
                x -> x.hasComponent(MultiBlockTankWallComponent.class),
                x -> true,
                "IRLCorp:MultiBlockTank",
                new MultiBlockCallback<Void>() {
                    @Override
                    public Map<Vector3i, Block> getReplacementMap(Region3i region, Void designDetails) {
                        return null;
                    }

                    @Override
                    public void multiBlockFormed(Region3i region, EntityRef entity, Void designDetails) {
                        double bonusFromFootprint = Math.pow(1.2, Math.min(region.sizeX(), region.sizeZ() * 0.5));
                        FluidInventoryComponent fluidInventoryComponent = new FluidInventoryComponent(
                                1,
                                (float) (region.size().x * region.size().y * region.size().z * 10000 * bonusFromFootprint));
                        if (entity.hasComponent(FluidInventoryComponent.class)) {
                            entity.saveComponent(fluidInventoryComponent);
                        } else {
                            entity.addComponent(fluidInventoryComponent);
                        }
                    }
                }
        );

        multiBlockFormRecipeRegistry.addMultiBlockFormItemRecipe(recipe);
    }

    @ReceiveEvent
    public void onActivatedActAsBlockAddHealth(OnActivatedComponent event,
                                               EntityRef entity,
                                               BlockRegionComponent blockRegion,
                                               ActAsBlockComponent actAsBlock,
                                               MultiBlockFluidTankComponent multiBlockFluidTank) {
        // Add health to the entity similarly to how block entities get health.
        if (actAsBlock.block != null && actAsBlock.block.getArchetypeBlock().isDestructible() && !entity.hasComponent(HealthComponent.class)) {
            HealthComponent healthComponent = new HealthComponent();

            // make health scale with size, use the actasblock as a starting value
            healthComponent.maxHealth =
                    (int) (actAsBlock.block.getArchetypeBlock().getHardness() * ((blockRegion.region.sizeX() * blockRegion.region.sizeY() * blockRegion.region.sizeZ()) / 4f));
            healthComponent.currentHealth = healthComponent.maxHealth;

            // Block regen should always take the same amount of time,  regardless of its hardness
            healthComponent.regenRate = actAsBlock.block.getArchetypeBlock().getHardness() / 4.0f;
            healthComponent.waitBeforeRegen = 1.0f;
            healthComponent.destroyEntityOnNoHealth = true;

            entity.addComponent(healthComponent);
        }
    }
}
