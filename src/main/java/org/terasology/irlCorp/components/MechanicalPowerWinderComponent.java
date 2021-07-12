// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerWinderComponent implements Component<MechanicalPowerWinderComponent> {
    public long recoveryTime;
    public int maxTransferAmount;

    @Override
    public void copy(MechanicalPowerWinderComponent other) {
        this.recoveryTime = other.recoveryTime;
        this.maxTransferAmount = other.maxTransferAmount;
    }
}
