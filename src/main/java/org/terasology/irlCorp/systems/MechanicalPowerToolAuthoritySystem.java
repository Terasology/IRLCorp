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
import com.google.common.collect.Maps;
import org.joml.RoundingMode;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector3ic;
import org.terasology.RotationUtils;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterMode;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.logic.characters.CharacterComponent;
import org.terasology.engine.logic.characters.GazeAuthoritySystem;
import org.terasology.engine.logic.common.ActivateEvent;
import org.terasology.engine.logic.inventory.ItemComponent;
import org.terasology.engine.logic.location.LocationComponent;
import org.terasology.engine.math.Direction;
import org.terasology.engine.math.Rotation;
import org.terasology.engine.math.Side;
import org.terasology.engine.math.SpiralIterable;
import org.terasology.engine.math.Yaw;
import org.terasology.engine.physics.CollisionGroup;
import org.terasology.engine.physics.HitResult;
import org.terasology.engine.physics.Physics;
import org.terasology.engine.physics.StandardCollisionGroup;
import org.terasology.engine.registry.In;
import org.terasology.engine.world.BlockEntityRegistry;
import org.terasology.engine.world.WorldProvider;
import org.terasology.engine.world.block.Block;
import org.terasology.engine.world.block.BlockComponent;
import org.terasology.engine.world.block.BlockManager;
import org.terasology.engine.world.block.entity.placement.PlaceBlocks;
import org.terasology.engine.world.block.family.BlockFamily;
import org.terasology.engine.world.block.family.BlockPlacementData;
import org.terasology.engine.world.block.items.BlockItemComponent;
import org.terasology.engine.world.block.items.OnBlockToItem;
import org.terasology.irlCorp.components.BlockPlacerAmmoChestComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolDamageAdjacentComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolIncreaseMaxPowerComponent;
import org.terasology.irlCorp.components.ToolBlockPlacementComponent;
import org.terasology.irlCorp.components.ToolDamageAdjacentComponent;
import org.terasology.irlCorp.events.BeforePowerToolUsedEvent;
import org.terasology.irlCorp.events.MechanicalPowerToolPartAddedEvent;
import org.terasology.irlCorp.events.PowerToolUsedEvent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.module.health.events.DoDamageEvent;
import org.terasology.module.inventory.systems.InventoryManager;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;

import java.util.List;
import java.util.Map;

@RegisterSystem(RegisterMode.AUTHORITY)
public class MechanicalPowerToolAuthoritySystem extends BaseComponentSystem {
    private static CollisionGroup[] filter = {StandardCollisionGroup.DEFAULT, StandardCollisionGroup.WORLD};

    @In
    BlockEntityRegistry blockEntityRegistry;
    @In
    Physics physics;
    @In
    InventoryManager inventoryManager;
    @In
    WorldProvider worldProvider;

    @ReceiveEvent
    public void addIncreaseMaxPower(MechanicalPowerToolPartAddedEvent event, EntityRef item, MechanicalPowerToolIncreaseMaxPowerComponent details) {
        EntityRef toolEntity = event.getToolEntity();
        PotentialEnergyDeviceComponent consumerComponent = toolEntity.getComponent(PotentialEnergyDeviceComponent.class);
        ItemComponent itemComponent = item.getComponent(ItemComponent.class);
        if (consumerComponent == null) {
            consumerComponent = new PotentialEnergyDeviceComponent();
            consumerComponent.maximumStoredEnergy = details.amount * itemComponent.stackCount;
            toolEntity.addComponent(consumerComponent);
        } else {
            consumerComponent.maximumStoredEnergy += details.amount * itemComponent.stackCount;
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

                    HitResult result = getHitResult(event.getInstigator(), physics);

                    if (result.isWorldHit()) {
                        // loop through each of the directions to damage, reorienting each with what side the block was hit on.
                        for (Vector3i relativePosition : damageAdjacentComponent.directions) {

                            Vector3i originPosition = blockComponent.getPosition(new Vector3i());
                            Vector3i adjacentPosition = RotationUtils.rotateVector3i(Direction.inDirection(result.getHitNormal()), relativePosition).add(originPosition);

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

    private static HitResult getHitResult(EntityRef instigator, Physics physics) {
        CharacterComponent characterComponent = instigator.getComponent(CharacterComponent.class);
        EntityRef eyeEntity = GazeAuthoritySystem.getGazeEntityForCharacter(instigator);
        LocationComponent location = eyeEntity.getComponent(LocationComponent.class);
        Vector3f direction = location.getWorldDirection(new Vector3f());
        Vector3f originPos = location.getWorldPosition(new Vector3f());
        return physics.rayTrace(originPos, direction, characterComponent.interactionRange, filter);
    }

    @ReceiveEvent
    public void canUsePowerTool(BeforePowerToolUsedEvent event, EntityRef toolEntity, MechanicalPowerToolComponent powerToolComponent) {
        if (!powerToolComponent.active) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void canUsePowerTool(BeforePowerToolUsedEvent event, EntityRef toolEntity, PotentialEnergyDeviceComponent consumerComponent) {
        float scaledAmount = (float) Math.pow(1.15, event.getAmount());
        if (scaledAmount > 0 && consumerComponent.currentStoredEnergy < scaledAmount) {
            event.consume();
        } else {
            event.setToolPowered();
        }
    }

    @ReceiveEvent
    public void usePowerTool(PowerToolUsedEvent event, EntityRef toolEntity, PotentialEnergyDeviceComponent consumerComponent) {
        float scaledAmount = (float) Math.pow(1.15, event.getAmount());
        consumerComponent.currentStoredEnergy -= scaledAmount;
        toolEntity.saveComponent(consumerComponent);
        event.consume();
    }

    @ReceiveEvent
    public void onUseBlockPlacementTool(ActivateEvent event, EntityRef tool, ToolBlockPlacementComponent blockPlacementComponent) {
        EntityRef instigator = event.getInstigator();

        List<Vector3i> positions = getPotentialBlockPlacementPositions(blockPlacementComponent, instigator, worldProvider, physics);
        if (positions.size() > 0) {
            float power = positions.size();
            BeforePowerToolUsedEvent beforePowerToolUsedEvent = new BeforePowerToolUsedEvent(power);
            tool.send(beforePowerToolUsedEvent);
            power = beforePowerToolUsedEvent.getAmount();

            if (!beforePowerToolUsedEvent.isConsumed() && beforePowerToolUsedEvent.isToolPowered() && instigator.hasComponent(CharacterComponent.class)) {
                EntityRef ammoChest = getAmmoChestForBlockPlacementTool(instigator, blockPlacementComponent, positions.size());
                EntityRef itemToPlace = getItemFromAmmoChest(ammoChest, positions.size());

                if (ammoChest.exists() && itemToPlace.exists()) {
                    if (inventoryManager.removeItem(ammoChest, tool, itemToPlace, true, positions.size()) != null) {
                        Map<Vector3i, Block> placementMap = Maps.newHashMap();
                        for (Vector3i position : positions) {
                            BlockItemComponent blockItemComponent = itemToPlace.getComponent(BlockItemComponent.class);
                            Block block = blockItemComponent.blockFamily.getBlockForPlacement(new BlockPlacementData(
                                position,
                                Side.inDirection(event.getHitNormal()).reverse(),
                                event.getDirection())
                            );
                            placementMap.put(position, block);
                        }

                        PlaceBlocks placeBlocks = new PlaceBlocks(placementMap, instigator);
                        worldProvider.getWorldEntity().send(placeBlocks);
                        if (!placeBlocks.isConsumed()) {
                            tool.send(new PowerToolUsedEvent(power));
                        }
                    }

                    event.consume();
                }
            }
        }
    }

    static List<Vector3i> getPotentialBlockPlacementPositions(ToolBlockPlacementComponent blockPlacementComponent, EntityRef characterEntity, WorldProvider worldProvider,
                                                              Physics physics) {
        List<Vector3i> positions = Lists.newLinkedList();

        HitResult hitResult = getHitResult(characterEntity, physics);
        if (hitResult.isWorldHit()) {
            BlockFamily buildOnBlockFamily = worldProvider.getBlock(hitResult.getBlockPosition()).getBlockFamily();

            for (Vector2ic pos : SpiralIterable.clockwise(new Vector2i()).maxRadius(blockPlacementComponent.maximumRange).build()) {
                Vector3i adjacentPositionToHitFace = RotationUtils.rotateVector3i(Direction.inDirection(hitResult.getHitNormal()), new Vector3i(pos.x(), pos.y(), 1))
                    .add(hitResult.getBlockPosition());
                Vector3i adjacentPosition = RotationUtils.rotateVector3i(Direction.inDirection(hitResult.getHitNormal()), new Vector3i(pos.x(), pos.y(), 0))
                    .add(hitResult.getBlockPosition());
                if (worldProvider.getBlock(adjacentPositionToHitFace).getBlockFamily().getURI().equals(BlockManager.AIR_ID)
                    && worldProvider.getBlock(adjacentPosition).getBlockFamily().equals(buildOnBlockFamily)
                        /* yes, this does cheat a bit.  It is likely good enough for simplicity sake.
                           Really this check should be done after we have gathered all possible positions. */
                    && connectsToExistingPosition(adjacentPositionToHitFace, positions)
                    && positions.size() < blockPlacementComponent.maximumBlocks) {
                    positions.add(adjacentPositionToHitFace);
                }
            }
        } else {
            CharacterComponent characterComponent = characterEntity.getComponent(CharacterComponent.class);
            LocationComponent locationComponent = GazeAuthoritySystem.getGazeEntityForCharacter(characterEntity).getComponent(LocationComponent.class);
            Vector3f direction = locationComponent.getWorldDirection(new Vector3f());
            Direction horizontalLookDirection = Direction.inDirection(direction.x, 0f, direction.z);
            if (direction.y < 0f) {
                // looking downwards, try and extend the ledge they are on
                LocationComponent characterLocationComponent = characterEntity.getComponent(LocationComponent.class);
                Vector3i blockUnderneath = new Vector3i(characterLocationComponent.getWorldPosition(new Vector3f()), RoundingMode.HALF_UP).sub(0, 1, 0);
                Vector3i previousBlockUnderneath = new Vector3i(blockUnderneath).sub(horizontalLookDirection.asVector3i());
                if (!worldProvider.getBlock(blockUnderneath).getBlockFamily().getURI().equals(BlockManager.AIR_ID)) {
                    for (int x = 0; x < characterComponent.interactionRange; x++) {
                        blockUnderneath.add(horizontalLookDirection.asVector3i());
                        previousBlockUnderneath.add(horizontalLookDirection.asVector3i());
                        if (worldProvider.getBlock(blockUnderneath).getBlockFamily().getURI().equals(BlockManager.AIR_ID)) {
                            // we have found some air within reach
                            positions.add(new Vector3i(blockUnderneath));

                            Rotation leftRotation = Rotation.rotate(Yaw.CLOCKWISE_270);
                            Vector3ic leftVector = leftRotation.rotate(horizontalLookDirection.toSide()).direction();
                            Vector3i currentPosition = new Vector3i(blockUnderneath);
                            Vector3i ledgePosition = new Vector3i(previousBlockUnderneath);
                            for (int left = 1; left < blockPlacementComponent.maximumRange && positions.size() < blockPlacementComponent.maximumBlocks; left++) {
                                currentPosition.add(leftVector);
                                ledgePosition.add(leftVector);
                                if (worldProvider.getBlock(currentPosition).getBlockFamily().getURI().equals(BlockManager.AIR_ID)
                                    && !worldProvider.getBlock(ledgePosition).getBlockFamily().getURI().equals(BlockManager.AIR_ID)) {
                                    positions.add(new Vector3i(currentPosition));
                                } else {
                                    break;
                                }
                            }

                            Rotation rightRotation = Rotation.rotate(Yaw.CLOCKWISE_90);
                            Vector3ic rightVector = rightRotation.rotate(horizontalLookDirection.toSide()).direction();
                            currentPosition = new Vector3i(blockUnderneath);
                            ledgePosition = new Vector3i(previousBlockUnderneath);
                            for (int right = 1; right < blockPlacementComponent.maximumRange && positions.size() < blockPlacementComponent.maximumBlocks; right++) {
                                currentPosition.add(rightVector);
                                ledgePosition.add(rightVector);
                                if (worldProvider.getBlock(currentPosition).getBlockFamily().getURI().equals(BlockManager.AIR_ID)
                                    && !worldProvider.getBlock(ledgePosition).getBlockFamily().getURI().equals(BlockManager.AIR_ID)) {
                                    positions.add(new Vector3i(currentPosition));
                                } else {
                                    break;
                                }
                            }

                            break;
                        }
                    }
                }

            }

        }

        return positions;
    }

    private static boolean connectsToExistingPosition(Vector3i position, List<Vector3i> existingPositions) {
        if (existingPositions.size() == 0) {
            // allow the first position to be valid
            return true;
        }

        for (Vector3i existingPosition : existingPositions) {
            if (position.gridDistance(existingPosition) == 1) {
                return true;
            }
        }
        return false;
    }

    private EntityRef getItemFromAmmoChest(EntityRef ammoChest, int itemsNeeded) {
        EntityRef targetItem = EntityRef.NULL;
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, ammoChest)) {
            BlockItemComponent blockItemComponent = item.getComponent(BlockItemComponent.class);
            if (blockItemComponent != null && inventoryManager.getStackSize(item) >= itemsNeeded) {
                targetItem = item;
                break;
            }
        }
        return targetItem;
    }

    private EntityRef getAmmoChestForBlockPlacementTool(EntityRef instigator, ToolBlockPlacementComponent blockPlacementComponent, int itemsNeeded) {
        EntityRef targetItem = EntityRef.NULL;
        for (EntityRef item : ExtendedInventoryManager.iterateItems(inventoryManager, instigator)) {
            BlockPlacerAmmoChestComponent blockPlacerAmmoChestComponent = item.getComponent(BlockPlacerAmmoChestComponent.class);
            if (blockPlacerAmmoChestComponent != null) {
                if (getItemFromAmmoChest(item, itemsNeeded).exists()) {
                    targetItem = item;
                    break;
                }
            }
        }
        return targetItem;
    }

    @ReceiveEvent
    public void retainBlockPlacerAmmoChestToItem(OnBlockToItem event, EntityRef entityRef, BlockPlacerAmmoChestComponent blockPlacerAmmoChestComponent) {
        event.getItem().addComponent(blockPlacerAmmoChestComponent);
    }
}
