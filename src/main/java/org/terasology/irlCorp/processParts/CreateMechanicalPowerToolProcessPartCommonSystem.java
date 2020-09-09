// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.processParts;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.ReceiveEvent;
import org.terasology.engine.entitySystem.systems.BaseComponentSystem;
import org.terasology.engine.entitySystem.systems.RegisterSystem;
import org.terasology.engine.registry.In;
import org.terasology.inventory.logic.InventoryManager;
import org.terasology.irlCorp.components.MechanicalPowerToolComponent;
import org.terasology.irlCorp.components.MechanicalPowerToolPartComponent;
import org.terasology.irlCorp.events.MechanicalPowerToolPartAddedEvent;
import org.terasology.machines.ExtendedInventoryManager;
import org.terasology.workstation.component.SpecificInputSlotComponent;
import org.terasology.workstation.process.WorkstationInventoryUtils;
import org.terasology.workstation.process.inventory.InventoryInputProcessPartCommonSystem;
import org.terasology.workstation.process.inventory.InventoryOutputProcessPartCommonSystem;
import org.terasology.workstation.processPart.ProcessEntityIsInvalidToStartEvent;
import org.terasology.workstation.processPart.ProcessEntityStartExecutionEvent;

@RegisterSystem
public class CreateMechanicalPowerToolProcessPartCommonSystem extends BaseComponentSystem {
    @In
    InventoryManager inventoryManager;

    ///// Processing

    @ReceiveEvent
    public void validateToStartExecution(ProcessEntityIsInvalidToStartEvent event, EntityRef processEntity,
                                         CreateMechanicalPowerToolComponent createMechanicalPowerToolComponent) {
        boolean hasParts = false;
        boolean hasTool = false;

        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, event.getWorkstation(),
                false, InventoryInputProcessPartCommonSystem.WORKSTATIONINPUTCATEGORY)) {
            if (entityRef.exists()) {
                if (entityRef.hasComponent(MechanicalPowerToolPartComponent.class)) {
                    hasParts = true;
                } else if (hasTool == false) {
                    processEntity.addComponent(new SpecificInputSlotComponent(inventoryManager.findSlotWithItem(event.getWorkstation(), entityRef)));
                    hasTool = true;
                }
            }
        }

        if (!(hasTool && hasParts)) {
            event.consume();
        }
    }

    @ReceiveEvent
    public void startExecution(ProcessEntityStartExecutionEvent event, EntityRef processEntity,
                               CreateMechanicalPowerToolComponent createMechanicalPowerToolComponent) {
        SpecificInputSlotComponent specificInputSlotComponent =
                processEntity.getComponent(SpecificInputSlotComponent.class);
        EntityRef item = inventoryManager.getItemInSlot(event.getWorkstation(), specificInputSlotComponent.slot);

        MechanicalPowerToolComponent powerToolComponent = new MechanicalPowerToolComponent();
        if (!item.hasComponent(MechanicalPowerToolComponent.class)) {
            item.addComponent(powerToolComponent);
        }

        // fire an event for all non-tool items,  removing them if successfully added the part
        for (EntityRef entityRef : ExtendedInventoryManager.iterateItems(inventoryManager, event.getWorkstation(),
                false, InventoryInputProcessPartCommonSystem.WORKSTATIONINPUTCATEGORY)) {
            if (entityRef.hasComponent(MechanicalPowerToolPartComponent.class) && !entityRef.equals(item)) {
                MechanicalPowerToolPartAddedEvent partAddedEvent = new MechanicalPowerToolPartAddedEvent(item);
                entityRef.send(partAddedEvent);
                if (partAddedEvent.isSuccess()) {
                    inventoryManager.removeItem(event.getWorkstation(), event.getInstigator(), entityRef, true, 1);
                }
            }
        }

        // put tool into the output slot
        inventoryManager.moveItemToSlots(
                event.getWorkstation(),
                event.getWorkstation(),
                specificInputSlotComponent.slot,
                event.getWorkstation(),
                WorkstationInventoryUtils.getAssignedSlots(event.getWorkstation(), true,
                        InventoryOutputProcessPartCommonSystem.WORKSTATIONOUTPUTCATEGORY));
    }
}
