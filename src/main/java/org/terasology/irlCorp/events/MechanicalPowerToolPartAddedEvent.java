// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.events;

import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.engine.entitySystem.event.Event;

public class MechanicalPowerToolPartAddedEvent implements Event {
    EntityRef toolEntity;
    boolean success;

    public MechanicalPowerToolPartAddedEvent(EntityRef toolEntity) {
        this.toolEntity = toolEntity;
    }

    public EntityRef getToolEntity() {
        return toolEntity;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }
}
