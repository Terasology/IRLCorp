/*
 * Copyright 2014 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.irlCorp.ui;

import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.machines.ui.VerticalProgressBar;
import org.terasology.math.Vector2i;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.CoreWidget;
import org.terasology.rendering.nui.LayoutConfig;
import org.terasology.rendering.nui.UIWidget;
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
            MechanicalPowerConsumerComponent consumer = station.getComponent(MechanicalPowerConsumerComponent.class);
            if (consumer != null) {
                float value = consumer.currentStoredPower / consumer.maximumStoredPower;
                powerMeter.setValue(value);
            }
        }
    }

    @Override
    public Vector2i getPreferredContentSize(Canvas canvas, Vector2i sizeHint) {
        return content.getPreferredContentSize(canvas, sizeHint);
    }
}
