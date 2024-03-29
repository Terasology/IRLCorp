// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerToolIncreaseMaxPowerComponent implements Component<MechanicalPowerToolIncreaseMaxPowerComponent> {
    public float amount = 0;

    @Override
    public void copyFrom(MechanicalPowerToolIncreaseMaxPowerComponent other) {
        this.amount = other.amount;
    }
}
