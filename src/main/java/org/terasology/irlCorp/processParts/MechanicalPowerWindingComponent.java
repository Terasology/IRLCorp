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
import org.terasology.irlCorp.components.MechanicalPowerWinderComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.registry.CoreRegistry;
import org.terasology.workstation.component.SpecificInputSlotComponent;
import org.terasology.workstation.process.ProcessPart;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryInputComponent;
import org.terasology.workstation.process.inventory.InventoryOutputComponent;

public class MechanicalPowerWindingComponent implements Component, ProcessPart {

    @Override
    public boolean validateBeforeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerWinderComponent winderComponent = workstation.getComponent(MechanicalPowerWinderComponent.class);
        if (winderComponent == null) {
            return false;
        }

        MechanicalPowerConsumerComponent consumerComponent = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        if (consumerComponent == null
                || consumerComponent.currentStoredPower < winderComponent.maxTransferAmount) {
            return false;
        }

        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);
        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, workstation, false, InventoryInputComponent.WORKSTATIONINPUTCATEGORY)) {
            MechanicalPowerConsumerComponent mechanicalPowerConsumerComponent = entityRef.getComponent(MechanicalPowerConsumerComponent.class);
            if (mechanicalPowerConsumerComponent != null) {
                processEntity.addComponent(new SpecificInputSlotComponent(inventoryManager.findSlotWithItem(workstation, entityRef)));
                return true;
            }
        }

        return false;
    }

    @Override
    public long getDuration(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        MechanicalPowerWinderComponent winder = workstation.getComponent(MechanicalPowerWinderComponent.class);
        if (winder != null) {
            return winder.recoveryTime;
        } else {
            return 0;
        }
    }

    @Override
    public void executeStart(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
        InventoryManager inventoryManager = CoreRegistry.get(InventoryManager.class);

        MechanicalPowerWinderComponent winderComponent = workstation.getComponent(MechanicalPowerWinderComponent.class);
        MechanicalPowerConsumerComponent consumerComponent = workstation.getComponent(MechanicalPowerConsumerComponent.class);
        SpecificInputSlotComponent specificInputSlotComponent = processEntity.getComponent(SpecificInputSlotComponent.class);
        EntityRef item = inventoryManager.getItemInSlot(workstation, specificInputSlotComponent.slot);
        MechanicalPowerConsumerComponent itemConsumerComponent = item.getComponent(MechanicalPowerConsumerComponent.class);
        if (winderComponent != null && consumerComponent != null && itemConsumerComponent != null) {
            float spaceAvailableInItem = itemConsumerComponent.maximumStoredPower - itemConsumerComponent.currentStoredPower;
            float amountToTransfer = Math.min(Math.min(winderComponent.maxTransferAmount, spaceAvailableInItem), consumerComponent.currentStoredPower);

            itemConsumerComponent.currentStoredPower += amountToTransfer;
            item.saveComponent(itemConsumerComponent);

            consumerComponent.currentStoredPower -= amountToTransfer;
            workstation.saveComponent(consumerComponent);
        }

        if (itemConsumerComponent.currentStoredPower == itemConsumerComponent.maximumStoredPower) {
            // this item is full,  put into the output
            inventoryManager.moveItemToSlots(
                    instigator,
                    workstation,
                    specificInputSlotComponent.slot,
                    workstation,
                    WorkstationInventoryUtils.getAssignedSlots(workstation, true, InventoryOutputComponent.WORKSTATIONOUTPUTCATEGORY));
        }
    }

    @Override
    public void executeEnd(EntityRef instigator, EntityRef workstation, EntityRef processEntity) {
    }
}
