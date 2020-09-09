// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.events;

import org.terasology.engine.entitySystem.event.AbstractConsumableEvent;

public class BeforePowerToolUsedEvent extends AbstractConsumableEvent {
    float amount;
    boolean toolPowered;

    public BeforePowerToolUsedEvent() {
    }

    public BeforePowerToolUsedEvent(float amount) {
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }

    public void setAmount(float amount) {
        this.amount = amount;
    }

    public void setToolPowered() {
        toolPowered = true;
    }

    public boolean isToolPowered() {
        return toolPowered;
    }
}

