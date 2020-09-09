// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.events;

import org.terasology.engine.entitySystem.event.AbstractConsumableEvent;

public class PowerToolUsedEvent extends AbstractConsumableEvent {
    float amount;

    public PowerToolUsedEvent() {
    }

    public PowerToolUsedEvent(float amount) {
        this.amount = amount;
    }

    public float getAmount() {
        return amount;
    }
}

