/*
 * Copyright 2015 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.irlCorp.systems;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockRegion;
import org.terasology.engine.world.block.regions.ActAsBlockComponent;
import org.terasology.engine.world.block.regions.BlockRegionComponent;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.gestalt.entitysystem.event.ReceiveEvent;
import org.terasology.irlCorp.components.MultiBlockActivatorComponent;
import org.terasology.irlCorp.components.MultiBlockFluidTankComponent;
import org.terasology.irlCorp.components.MultiBlockTankWallComponent;
import org.terasology.module.health.components.HealthComponent;
import org.terasology.module.health.core.BaseRegenComponent;
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
                    public Map<org.joml.Vector3i, Block> getReplacementMap(BlockRegion region, Void designDetails) {
                        return null;
                    }

                    @Override
                    public void multiBlockFormed(BlockRegion region, EntityRef entity, Void designDetails) {
                        double bonusFromFootprint = Math.pow(1.2, Math.min(region.getSizeX(), region.getSizeZ() * 0.5));
                        FluidInventoryComponent fluidInventoryComponent = new FluidInventoryComponent(
                                1,
                                (float) (region.getSizeX() * region.getSizeY() * region.getSizeZ() * 10000 * bonusFromFootprint));
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
        if (actAsBlock.block != null && actAsBlock.block.getArchetypeBlock().isDestructible()) {
            if (!entity.hasComponent(HealthComponent.class)) {
                HealthComponent healthComponent = new HealthComponent();

                // make health scale with size, use the actasblock as a starting value
                healthComponent.maxHealth =
                        (int) (actAsBlock.block.getArchetypeBlock().getHardness() * ((blockRegion.region.getSizeX() * blockRegion.region.getSizeY() * blockRegion.region.getSizeZ()) / 4f));
                healthComponent.currentHealth = healthComponent.maxHealth;
                healthComponent.destroyEntityOnNoHealth = true;

                entity.addComponent(healthComponent);
            }

            if (!entity.hasComponent(BaseRegenComponent.class)) {
                BaseRegenComponent baseRegenComponent = new BaseRegenComponent();
                baseRegenComponent.regenRate = actAsBlock.block.getArchetypeBlock().getHardness() / 4.0f;
                baseRegenComponent.waitBeforeRegen = 1f;

                entity.addComponent(baseRegenComponent);
            }
        }
    }
}
