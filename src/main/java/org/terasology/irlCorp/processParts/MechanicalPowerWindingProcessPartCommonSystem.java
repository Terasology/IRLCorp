/*
 * Copyright 2016 MovingBlocks
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

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.irlCorp.components.MechanicalPowerWinderComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.registry.In;
import org.terasology.workstation.component.SpecificInputSlotComponent;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryInputProcessPartCommonSystem;
import org.terasology.workstation.process.inventory.InventoryOutputProcessPartCommonSystem;
import org.terasology.workstation.processPart.ProcessEntityGetDurationEvent;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;

@RegisterSystem
public class MechanicalPowerWindingProcessPartCommonSystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         MechanicalPowerWindingComponent mechanicalPowerWindingComponent) {
        MechanicalPowerWinderComponent winderComponent = event.getWorkstation().getComponent(MechanicalPowerWinderComponent.class);
        if (winderComponent == null) {
            event.consume();
            return;
        }

        MechanicalPowerConsumerComponent consumerComponent = event.getWorkstation().getComponent(MechanicalPowerConsumerComponent.class);
        if (consumerComponent == null
                || consumerComponent.currentStoredPower < winderComponent.maxTransferAmount) {
            event.consume();
            return;
        }

        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, event.getWorkstation(), false, InventoryInputProcessPartCommonSystem.WORKSTATIONINPUTCATEGORY)) {
            MechanicalPowerConsumerComponent mechanicalPowerConsumerComponent = entityRef.getComponent(MechanicalPowerConsumerComponent.class);
            if (mechanicalPowerConsumerComponent != null) {
                processEntity.addComponent(new SpecificInputSlotComponent(inventoryManager.findSlotWithItem(event.getWorkstation(), entityRef)));
                return;
            }
        }

        event.consume();
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               MechanicalPowerWindingComponent mechanicalPowerWindingComponent) {
        EntityRef workstation = event.getWorkstation();
        EntityRef instigator = event.getInstigator();

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
                    WorkstationInventoryUtils.getAssignedSlots(workstation, true, InventoryOutputProcessPartCommonSystem.WORKSTATIONOUTPUTCATEGORY));
        }
    }

    @ReceiveEvent
    public void getDuration(ProcessEntityGetDurationEvent event, EntityRef processEntity,
                            MechanicalPowerWindingComponent mechanicalPowerWindingComponent) {
        MechanicalPowerWinderComponent winder = event.getWorkstation().getComponent(MechanicalPowerWinderComponent.class);
        if (winder != null) {
            event.add(winder.recoveryTime / 1000f);
        }
    }
}
