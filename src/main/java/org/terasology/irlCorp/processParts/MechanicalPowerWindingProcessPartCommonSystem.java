// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.processParts;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.inventory.logic.InventoryManager;
import org.terasology.irlCorp.components.MechanicalPowerWinderComponent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
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
        MechanicalPowerWinderComponent winderComponent =
                event.getWorkstation().getComponent(MechanicalPowerWinderComponent.class);
        if (winderComponent == null) {
            event.consume();
            return;
        }

        PotentialEnergyDeviceComponent consumerComponent =
                event.getWorkstation().getComponent(PotentialEnergyDeviceComponent.class);
        if (consumerComponent == null
                || consumerComponent.currentStoredEnergy < winderComponent.maxTransferAmount) {
            event.consume();
            return;
        }

        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, event.getWorkstation(),
                false, InventoryInputProcessPartCommonSystem.WORKSTATIONINPUTCATEGORY)) {
            PotentialEnergyDeviceComponent mechanicalPowerConsumerComponent =
                    entityRef.getComponent(PotentialEnergyDeviceComponent.class);
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
        PotentialEnergyDeviceComponent consumerComponent =
                workstation.getComponent(PotentialEnergyDeviceComponent.class);
        SpecificInputSlotComponent specificInputSlotComponent =
                processEntity.getComponent(SpecificInputSlotComponent.class);
        EntityRef item = inventoryManager.getItemInSlot(workstation, specificInputSlotComponent.slot);
        PotentialEnergyDeviceComponent itemConsumerComponent = item.getComponent(PotentialEnergyDeviceComponent.class);
        if (winderComponent != null && consumerComponent != null && itemConsumerComponent != null) {
            float spaceAvailableInItem =
                    itemConsumerComponent.maximumStoredEnergy - itemConsumerComponent.currentStoredEnergy;
            float amountToTransfer = Math.min(Math.min(winderComponent.maxTransferAmount, spaceAvailableInItem),
                    consumerComponent.currentStoredEnergy);

            itemConsumerComponent.currentStoredEnergy += amountToTransfer;
            item.saveComponent(itemConsumerComponent);

            consumerComponent.currentStoredEnergy -= amountToTransfer;
            workstation.saveComponent(consumerComponent);
        }

        if (itemConsumerComponent.currentStoredEnergy == itemConsumerComponent.maximumStoredEnergy) {
            // this item is full,  put into the output
            inventoryManager.moveItemToSlots(
                    instigator,
                    workstation,
                    specificInputSlotComponent.slot,
                    workstation,
                    WorkstationInventoryUtils.getAssignedSlots(workstation, true,
                            InventoryOutputProcessPartCommonSystem.WORKSTATIONOUTPUTCATEGORY));
        }
    }

    @ReceiveEvent
    public void getDuration(ProcessEntityGetDurationEvent event, EntityRef processEntity,
                            MechanicalPowerWindingComponent mechanicalPowerWindingComponent) {
        MechanicalPowerWinderComponent winder =
                event.getWorkstation().getComponent(MechanicalPowerWinderComponent.class);
        if (winder != null) {
            event.add(winder.recoveryTime / 1000f);
        }
    }
}
