// Copyright 2021 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.components;

import com.google.common.collect.Lists;
import org.joml.Vector3i;
import org.terasology.gestalt.entitysystem.component.Component;

import java.util.List;

public class ToolDamageAdjacentComponent implements Component<ToolDamageAdjacentComponent> {
    public static Vector3i[] orderedDirections = {
            new Vector3i(0, -1, 0),
            new Vector3i(-1, 0, 0),
            new Vector3i(1, 0, 0),
            new Vector3i(0, 1, 0),
            new Vector3i(-1, -1, 0),
            new Vector3i(-1, 1, 0),
            new Vector3i(1, 1, 0),
            new Vector3i(1, -1, 0)
    };

    public List<Vector3i> directions = Lists.newArrayList();
}
