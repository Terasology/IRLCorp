// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;

public class MechanicalPowerToolComponent implements Component {
    @Replicate
    public Boolean active = true;
}
