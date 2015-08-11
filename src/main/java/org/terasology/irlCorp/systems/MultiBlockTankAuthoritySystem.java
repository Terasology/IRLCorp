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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.entity.lifecycleEvents.OnActivatedComponent;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.fluid.component.FluidInventoryComponent;
import org.terasology.irlCorp.components.MultiBlockActivatorComponent;
import org.terasology.irlCorp.components.MultiBlockFluidTankComponent;
import org.terasology.irlCorp.components.MultiBlockTankWallComponent;
import org.terasology.logic.health.HealthComponent;
import org.terasology.math.Region3i;
import org.terasology.math.geom.Vector3i;
import org.terasology.multiBlock.MultiBlockCallback;
import org.terasology.multiBlock.MultiBlockFormRecipeRegistry;
import org.terasology.multiBlock.recipe.MultiBlockFormItemRecipe;
import org.terasology.multiBlock.recipe.UniformMultiBlockFormItemRecipe;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.Block;
import org.terasology.world.block.regions.ActAsBlockComponent;
import org.terasology.world.block.regions.BlockRegionComponent;

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
            // Block regen should always take the same amount of time,  regardless of its hardness
            HealthComponent healthComponent = new HealthComponent(
                    // make health scale with size, use the actasblock as a starting value
                    (int) (actAsBlock.block.getArchetypeBlock().getHardness() * ((blockRegion.region.sizeX() * blockRegion.region.sizeY() * blockRegion.region.sizeZ()) / 4f)),
                    actAsBlock.block.getArchetypeBlock().getHardness() / 4.0f,
                    1.0f);
            healthComponent.destroyEntityOnNoHealth = true;
            entity.addComponent(healthComponent);
        }
    }
}
