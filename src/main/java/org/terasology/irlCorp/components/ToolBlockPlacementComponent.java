// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.engine.network.Replicate;
import org.terasology.gestalt.entitysystem.component.Component;

public class ToolBlockPlacementComponent implements Component<ToolBlockPlacementComponent> {
    @Replicate
    public int maximumRange = 5;
    @Replicate
    public int maximumBlocks = 10;
}
