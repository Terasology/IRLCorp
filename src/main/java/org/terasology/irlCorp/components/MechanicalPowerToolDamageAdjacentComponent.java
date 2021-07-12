// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerToolDamageAdjacentComponent implements Component<MechanicalPowerToolDamageAdjacentComponent> {
    public int amount = 1;

    @Override
    public void copy(MechanicalPowerToolDamageAdjacentComponent other) {
        this.amount = other.amount;
    }
}
