/*
 * Copyright 2015 MovingBlocks
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
package org.terasology.irlCorp.systems;

import org.terasology.asset.Assets;
import org.terasology.assets.ResourceUrn;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.entitySystem.systems.RenderSystem;
import org.terasology.irlCorp.components.MechanicalPowerToolComponent;
import org.terasology.irlCorp.components.ToolBlockPlacementComponent;
import org.terasology.irlCorp.components.ToolDamageAdjacentComponent;
import org.terasology.logic.characters.CharacterComponent;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Rect2i;
import org.terasology.math.geom.Vector2i;
import org.terasology.math.geom.Vector3i;
import org.terasology.mechanicalPower.components.MechanicalPowerConsumerComponent;
import org.terasology.physics.Physics;
import org.terasology.registry.In;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureUtil;
import org.terasology.rendering.nui.Canvas;
import org.terasology.rendering.nui.Color;
import org.terasology.rendering.nui.layers.ingame.inventory.GetItemTooltip;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryCellRendered;
import org.terasology.rendering.nui.widgets.TooltipLine;
import org.terasology.rendering.world.selection.BlockSelectionRenderer;
import org.terasology.world.WorldProvider;

@RegisterSystem(RegisterMode.CLIENT)
public class MechanicalPowerToolClientSystem extends BaseComponentSystem implements RenderSystem {
    @In
    LocalPlayer localPlayer;
    @In
    Physics physics;
    @In
    WorldProvider worldProvider;
    @In
    InventoryManager inventoryManager;

    BlockSelectionRenderer blockSelectionRenderer;

    @ReceiveEvent
    public void drawPowerToolPowerBar(InventoryCellRendered event, EntityRef entity,
                                      MechanicalPowerToolComponent powerToolComponent,
                                      MechanicalPowerConsumerComponent powerConsumerComponent) {
        Canvas canvas = event.getCanvas();

        Vector2i size = canvas.size();

        int minX = (int) (size.x * 0.1f);
        int maxX = (int) (size.x * 0.9f);

        int minY = (int) (size.y * 0.7f);
        int maxY = (int) (size.y * 0.8f);

        float percentage = 1f * powerConsumerComponent.currentStoredPower / powerConsumerComponent.maximumStoredPower;

        ResourceUrn backgroundTexture = TextureUtil.getTextureUriForColor(Color.WHITE);

        final Color terasologyColor = powerToolComponent.active ? Color.GREEN : Color.GREY;


        ResourceUrn barTexture = TextureUtil.getTextureUriForColor(terasologyColor);

        canvas.drawTexture(Assets.get(backgroundTexture, Texture.class).get(), Rect2i.createFromMinAndMax(minX, minY, maxX, maxY));
        int barLength = (int) (percentage * (maxX - minX - 1));
        int barHeight = maxY - minY - 1;
        canvas.drawTexture(Assets.get(barTexture, Texture.class).get(), Rect2i.createFromMinAndSize(minX + 1, minY + 1, barLength, barHeight));
    }

    @ReceiveEvent
    public void getItemTooltipDamageAdjacent(GetItemTooltip event, EntityRef entityRef, ToolDamageAdjacentComponent damageAdjacentComponent) {
        event.getTooltipLines().add(new TooltipLine("Damages " + damageAdjacentComponent.directions.size() + " adjacent blocks"));
    }

    @ReceiveEvent
    public void getItemTooltipBlockPlacement(GetItemTooltip event, EntityRef entityRef, ToolBlockPlacementComponent blockPlacementComponent) {
        String blockName = blockPlacementComponent.sourceBlockFamily == null ? "air" : blockPlacementComponent.sourceBlockFamily.getDisplayName();
        event.getTooltipLines().add(new TooltipLine("Places " + blockName + " blocks"));
    }


    @Override
    public void renderOpaque() {

    }

    @Override
    public void renderAlphaBlend() {

    }

    @Override
    public void renderOverlay() {
        CharacterComponent characterComponent = localPlayer.getCharacterEntity().getComponent(CharacterComponent.class);
        EntityRef selectedItem = inventoryManager.getItemInSlot(localPlayer.getCharacterEntity(), characterComponent.selectedItem);
        ToolBlockPlacementComponent blockPlacementComponent = selectedItem.getComponent(ToolBlockPlacementComponent.class);
        if (blockPlacementComponent != null) {
            if (blockSelectionRenderer == null) {
                Texture texture = Assets.getTexture("engine:selection").get();
                blockSelectionRenderer = new BlockSelectionRenderer(texture);
            }
            blockSelectionRenderer.beginRenderOverlay();

            for (Vector3i position : MechanicalPowerToolAuthoritySystem.getPotentialBlockPlacementPositions(blockPlacementComponent, localPlayer.getCharacterEntity(), worldProvider, physics)) {
                blockSelectionRenderer.renderMark(position);
            }

            blockSelectionRenderer.endRenderOverlay();
        }

    }

    @Override
    public void renderFirstPerson() {

    }

    @Override
    public void renderShadows() {

    }
}
