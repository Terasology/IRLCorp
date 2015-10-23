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
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.irlCorp.components.MechanicalPowerToolDamageAdjacentComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolIncreaseMaxPowerComponent;
import org.terasology.irlCorp.events.MechanicalPowerToolPartAddedEvent;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.math.geom.Vector3i;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MechanicalPowerToolAuthoritySystem extends BaseComponentSystem {
    @In
    BlockEntityRegistry blockEntityRegistry;

    @ReceiveEvent
    public void addIncreaseMaxPower(MechanicalPowerToolPartAddedEvent event, EntityRef item, MechanicalPowerToolIncreaseMaxPowerComponent details) {
        EntityRef toolEntity = event.getToolEntity();
        MechanicalPowerConsumerComponent consumerComponent = toolEntity.getComponent(MechanicalPowerConsumerComponent.class);
        ItemComponent itemComponent = item.getComponent(ItemComponent.class);
        if (consumerComponent == null) {
            consumerComponent = new MechanicalPowerConsumerComponent();
            consumerComponent.maximumStoredPower = details.amount * itemComponent.stackCount;
            toolEntity.addComponent(consumerComponent);
        } else {
            consumerComponent.maximumStoredPower += details.amount * itemComponent.stackCount;
            toolEntity.saveComponent(consumerComponent);
        }
        event.setSuccess(true);
    }

    @ReceiveEvent
    public void addDamageAdjacent(MechanicalPowerToolPartAddedEvent event, EntityRef item, MechanicalPowerToolDamageAdjacentComponent details) {
        EntityRef toolEntity = event.getToolEntity();
        if (toolEntity.hasComponent(MechanicalPowerToolDamageAdjacentComponent.class)) {
            toolEntity.saveComponent(details);
        } else {
            toolEntity.addComponent(details);
        }
        event.setSuccess(true);
    }

    @ReceiveEvent
    public void damageAdjacent(DoDamageEvent event, EntityRef entityRef, BlockComponent blockComponent) {
        EntityRef toolEntity = event.getDirectCause();
        // only do adjacent damage if it is not the tool instigating the damage (would cause a loop)
        if (!toolEntity.equals(event.getInstigator())) {
            MechanicalPowerToolDamageAdjacentComponent damageAdjacentComponent = toolEntity.getComponent(MechanicalPowerToolDamageAdjacentComponent.class);
            if (damageAdjacentComponent != null) {
                float power = damageAdjacentComponent.directions.size();
                if (canUsePowerTool(toolEntity, power)) {
                    for (Vector3i relativePosition : damageAdjacentComponent.directions) {
                        // TODO: use something from the player to determine orientation
                        Vector3i adjacentPosition = new Vector3i(blockComponent.getPosition());
                        adjacentPosition.add(relativePosition);
                        EntityRef adjacentEntityRef = blockEntityRegistry.getBlockEntityAt(adjacentPosition);
                        DoDamageEvent adjacentEvent = new DoDamageEvent(event.getAmount(), event.getDamageType(), toolEntity, toolEntity);
                        adjacentEntityRef.send(adjacentEvent);
                    }

                    usePowerTool(toolEntity, power);
                }
            }
        }
    }

    private static boolean canUsePowerTool(EntityRef toolEntity, float powerRequested) {
        MechanicalPowerConsumerComponent consumerComponent = toolEntity.getComponent(MechanicalPowerConsumerComponent.class);
        if (powerRequested > 0 && consumerComponent != null) {
            return consumerComponent.currentStoredPower >= powerRequested;
        }

        return false;
    }

    private static void usePowerTool(EntityRef toolEntity, float powerUsed) {
        MechanicalPowerConsumerComponent consumerComponent = toolEntity.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumerComponent != null) {
            consumerComponent.currentStoredPower -= powerUsed;
            toolEntity.saveComponent(consumerComponent);
        }
    }
}
