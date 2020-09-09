// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import org.terasology.engine.entitySystem.Component;
import org.terasology.engine.network.Replicate;

public class ToolBlockPlacementComponent implements Component {
    @Replicate
    public int maximumRange = 5;
    @Replicate
    public int maximumBlocks = 10;
}
