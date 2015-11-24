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
package org.terasology.irlCorp.processParts;

import org.terasology.entitySystem.Component;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.irlCorp.components.MechanicalPowerToolComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolPartComponent;
import org.terasology.irlCorp.events.MechanicalPowerToolPartAddedEvent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.registry.CoreRegistry;
import org.terasology.workstation.component.SpecificInputSlotComponent;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryInputComponent;
import org.terasology.workstation.process.inventory.InventoryOutputComponent;
import org.terasology.workstation.process.inventory.ValidateInventoryItem;

public class CreateMechanicalPowerToolComponent implements Component, ProcessPart, ValidateInventoryItem {

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        boolean hasParts = false;
        boolean hasTool = false;

        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, false, InventoryInputComponent.WORKSTATIONINPUTCATEGORY)) {
            if (entityRef.exists()) {
                if (entityRef.hasComponent(MechanicalPowerToolPartComponent.class)) {
                    hasParts = true;
                } else if (hasTool == false) {
                    processEntity.addComponent(new SpecificInputSlotComponent(inventoryManager.findSlotWithItem(workstation, entityRef)));
                    hasTool = true;
                }
            }
        }

        return hasTool && hasParts;
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        return 0;
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        SpecificInputSlotComponent specificInputSlotComponent = processEntity.getComponent(SpecificInputSlotComponent.class);
        EntityRef item = inventoryManager.getItemInSlot(workstation, specificInputSlotComponent.slot);

        MechanicalPowerToolComponent powerToolComponent = new MechanicalPowerToolComponent();
        if (!item.hasComponent(MechanicalPowerToolComponent.class)) {
            item.addComponent(powerToolComponent);
        }

        // fire an event for all non-tool items,  removing them if successfully added the part
        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, false, InventoryInputComponent.WORKSTATIONINPUTCATEGORY)) {
            if (entityRef.hasComponent(MechanicalPowerToolPartComponent.class) && !entityRef.equals(item)) {
                MechanicalPowerToolPartAddedEvent event = new MechanicalPowerToolPartAddedEvent(item);
                entityRef.send(event);
                if (event.isSuccess()) {
                    inventoryManager.removeItem(workstation, instigator, entityRef, true, 1);
                }
            }
        }

        // put tool into the output slot
        inventoryManager.moveItemToSlots(
                workstation,
                workstation,
                specificInputSlotComponent.slot,
                workstation,
                WorkstationInventoryUtils.getAssignedSlots(workstation, true, InventoryOutputComponent.WORKSTATIONOUTPUTCATEGORY));
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
    }

    @Override
    public boolean isResponsibleForSlot(EntityRef workstation, int slotNo) {
        return WorkstationInventoryUtils.getAssignedInputSlots(workstation, InventoryInputComponent.WORKSTATIONINPUTCATEGORY).contains(slotNo);
    }

    @Override
    public boolean isValid(EntityRef workstation, int slotNo, EntityRef instigator, EntityRef item) {
        // allow any items to become power tools
        return true;
    }
}
