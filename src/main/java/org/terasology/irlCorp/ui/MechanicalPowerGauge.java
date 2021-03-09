// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.ui;

import org.joml.Vector2i;
import org.terasology.engine.entitySystem.entity.EntityRef;
import org.terasology.machines.ui.VerticalProgressBar;
import org.terasology.nui.Canvas;
import org.terasology.nui.CoreWidget;
import org.terasology.nui.LayoutConfig;
import org.terasology.nui.UIWidget;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
import org.terasology.workstation.ui.WorkstationUI;

public class MechanicalPowerGauge extends CoreWidget implements WorkstationUI {
    private EntityRef station;

    @LayoutConfig
    private UIWidget content;

    @Override
    public void initializeWorkstation(EntityRef entity) {
        station = entity;
    }

    @Override
    public void onDraw(Canvas canvas) {
        canvas.drawWidget(content);
    }

    @Override
    public void update(float delta) {
        super.update(delta);

        if (content instanceof VerticalProgressBar && station.exists()) {
            VerticalProgressBar powerMeter = (VerticalProgressBar) content;
            PotentialEnergyDeviceComponent consumer = station.getComponent(PotentialEnergyDeviceComponent.class);
            if (consumer != null) {
                float value = consumer.currentStoredEnergy / consumer.maximumStoredEnergy;
                powerMeter.setValue(value);

                setTooltipDelay(0);
                setTooltip(String.format("Power: %.0f/%.0f", consumer.currentStoredEnergy, consumer.maximumStoredEnergy));
            }
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return content == null ? sizeHint : content.getPreferredContentSize(canvas, sizeHint);
    }
}
