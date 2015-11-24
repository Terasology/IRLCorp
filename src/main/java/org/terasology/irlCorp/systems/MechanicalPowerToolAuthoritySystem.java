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

import com.google.common.collect.Lists;
import org.terasology.RotationUtils;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.irlCorp.components.MechanicalPowerToolComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolDamageAdjacentComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolIncreaseMaxPowerComponent;
import org.terasology.irlCorp.components.ToolDamageAdjacentComponent;
import org.terasology.irlCorp.events.BeforePowerToolUsedEvent;
import org.terasology.irlCorp.events.MechanicalPowerToolPartAddedEvent;
import org.terasology.irlCorp.events.PowerToolUsedEvent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.health.DoDamageEvent;
import org.terasology.logic.inventory.ItemComponent;
import org.terasology.logic.location.LocationComponent;
import org.terasology.math.Direction;
import org.terasology.math.Rotation;
import org.terasology.math.geom.Vector3f;
import org.terasology.math.geom.Vector3i;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.physics.CollisionGroup;
import org.terasology.physics.HitResult;
import org.terasology.physics.Physics;
import org.terasology.physics.StandardCollisionGroup;
import org.terasology.registry.In;
import org.terasology.world.BlockEntityRegistry;
import org.terasology.world.block.BlockComponent;

import java.util.List;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MechanicalPowerToolAuthoritySystem extends BaseComponentSystem {
    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    Physics physics;

    private CollisionGroup[] filter = {StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD};

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
        int numberOfDirections = details.amount;

        EntityRef toolEntity = event.getToolEntity();
        ToolDamageAdjacentComponent toolDmgComponent = toolEntity.getComponent(ToolDamageAdjacentComponent.class);
        if (toolDmgComponent == null) {
            toolDmgComponent = new ToolDamageAdjacentComponent();
        }
        numberOfDirections += toolDmgComponent.directions.size();

        List<Vector3i> newDirections = Lists.newLinkedList();
        for (int i = 0; i < numberOfDirections && i <= ToolDamageAdjacentComponent.orderedDirections.length; i++) {
            newDirections.add(ToolDamageAdjacentComponent.orderedDirections[i]);
        }
        toolDmgComponent.directions = newDirections;

        if (toolEntity.hasComponent(MechanicalPowerToolDamageAdjacentComponent.class)) {
            toolEntity.saveComponent(toolDmgComponent);
        } else {
            toolEntity.addComponent(toolDmgComponent);
        }
        event.setSuccess(true);
    }

    @ReceiveEvent
    public void damageAdjacent(DoDamageEvent event, EntityRef entityRef, BlockComponent blockComponent) {
        EntityRef toolEntity = event.getDirectCause();
        // only do adjacent damage if it is not the tool instigating the damage (would cause a loop)
        if (!toolEntity.equals(event.getInstigator())) {
            ToolDamageAdjacentComponent damageAdjacentComponent = toolEntity.getComponent(ToolDamageAdjacentComponent.class);
            if (damageAdjacentComponent != null) {
                float power = damageAdjacentComponent.directions.size();

                BeforePowerToolUsedEvent beforePowerToolUsedEvent = new BeforePowerToolUsedEvent(power);
                toolEntity.send(beforePowerToolUsedEvent);
                power = beforePowerToolUsedEvent.getAmount();

                if (!beforePowerToolUsedEvent.isConsumed() && beforePowerToolUsedEvent.isToolPowered() && event.getInstigator().hasComponent(CharacterComponent.class)) {

                    // Copied from LocalPlayer.activateTargetOrOwnedEntity(...)
                    LocationComponent location = event.getInstigator().getComponent(LocationComponent.class);
                    CharacterComponent characterComponent = event.getInstigator().getComponent(CharacterComponent.class);
                    Vector3f direction = characterComponent.getLookDirection();
                    Vector3f originPos = location.getWorldPosition();
                    originPos.y += characterComponent.eyeOffset;
                    HitResult result = physics.rayTrace(originPos, direction, characterComponent.interactionRange, filter);

                    if (result.isWorldHit()) {
                        // loop through each of the directions to damage, reorienting each with what side the block was hit on.
                        for (Vector3i relativePosition : damageAdjacentComponent.directions) {
                            Direction targetedFace = Direction.inDirection(result.getHitNormal());
                            Rotation rotation = RotationUtils.getRotation(targetedFace.toSide());
                            Vector3i rotatedPosition = Vector3i.zero();
                            if (relativePosition.x != 0) {
                                rotatedPosition.add(rotation.rotate(Direction.inDirection(relativePosition.x, 0, 0).toSide()).getVector3i());
                            }
                            if (relativePosition.y != 0) {
                                rotatedPosition.add(rotation.rotate(Direction.inDirection(0, relativePosition.y, 0).toSide()).getVector3i());
                            }
                            if (relativePosition.z != 0) {
                                rotatedPosition.add(rotation.rotate(Direction.inDirection(0, 0, relativePosition.z).toSide()).getVector3i());
                            }
                            Vector3i adjacentPosition = new Vector3i(blockComponent.getPosition());
                            adjacentPosition.add(rotatedPosition);

                            EntityRef adjacentEntityRef = blockEntityRegistry.getBlockEntityAt(adjacentPosition);
                            DoDamageEvent adjacentEvent = new DoDamageEvent(event.getAmount(), event.getDamageType(), toolEntity, toolEntity);
                            adjacentEntityRef.send(adjacentEvent);
                        }

                        toolEntity.send(new PowerToolUsedEvent(power));
                    }
                }
            }
        }
    }

    @ReceiveEvent
    public void canUsePowerTool(BeforePowerToolUsedEvent event, EntityRef toolEntity, MechanicalPowerToolComponent powerToolComponent) {
        if (!powerToolComponent.active) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void canUsePowerTool(BeforePowerToolUsedEvent event, EntityRef toolEntity, MechanicalPowerConsumerComponent consumerComponent) {
        if (event.getAmount() > 0 && consumerComponent.currentStoredPower < event.getAmount()) {
            event.consume();
        } else {
            event.setToolPowered();
        }
    }

    @ReceiveEvent
    public void usePowerTool(PowerToolUsedEvent event, EntityRef toolEntity, MechanicalPowerConsumerComponent consumerComponent) {
        consumerComponent.currentStoredPower -= Math.pow(1.15, event.getAmount());
        toolEntity.saveComponent(consumerComponent);
        event.consume();
    }
}
