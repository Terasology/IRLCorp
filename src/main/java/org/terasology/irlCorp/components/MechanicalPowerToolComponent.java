// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

public class MechanicalPowerToolComponent implements Component<MechanicalPowerToolComponent> {
    @Replicate
    public Boolean active = true;

    @Override
    public void copy(MechanicalPowerToolComponent other) {
        this.active = other.active;
    }
}
