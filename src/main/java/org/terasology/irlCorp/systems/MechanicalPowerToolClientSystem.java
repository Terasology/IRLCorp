// Copyright 2020 The Terasology Foundation
// SPDX-License-Identifier: Apache-2.0
package org.terasology.irlCorp.systems;

import org.joml.primitives.Rectanglei;
import org.joml.Vector2i;
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
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.inventory.SelectedInventorySlotComponent;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.math.geom.Vector3i;
import org.terasology.nui.Canvas;
import org.terasology.nui.Color;
import org.terasology.nui.util.RectUtility;
import org.terasology.nui.widgets.TooltipLine;
import org.terasology.physics.Physics;
import org.terasology.potentialEnergyDevices.components.PotentialEnergyDeviceComponent;
import org.terasology.registry.In;
import org.terasology.rendering.assets.texture.Texture;
import org.terasology.rendering.assets.texture.TextureUtil;
import org.terasology.rendering.nui.layers.ingame.inventory.GetItemTooltip;
import org.terasology.rendering.nui.layers.ingame.inventory.InventoryCellRendered;
import org.terasology.rendering.world.selection.BlockSelectionRenderer;
import org.terasology.utilities.Assets;
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
                                      PotentialEnergyDeviceComponent powerConsumerComponent) {
        Canvas canvas = event.getCanvas();

        Vector2i size = canvas.size();

        int minX = (int) (size.x * 0.1f);
        int maxX = (int) (size.x * 0.9f);

        int minY = (int) (size.y * 0.7f);
        int maxY = (int) (size.y * 0.8f);

        float percentage = 1f * powerConsumerComponent.currentStoredEnergy / powerConsumerComponent.maximumStoredEnergy;

        ResourceUrn backgroundTexture = TextureUtil.getTextureUriForColor(Color.WHITE);

        final Color terasologyColor = powerToolComponent.active ? Color.GREEN : Color.GREY;


        ResourceUrn barTexture = TextureUtil.getTextureUriForColor(terasologyColor);

        canvas.drawTexture(Assets.get(backgroundTexture, Texture.class).get(), new Rectanglei(minX, minY, maxX, maxY));
        int barLength = (int) (percentage * (maxX - minX - 1));
        int barHeight = maxY - minY - 1;
        canvas.drawTexture(Assets.get(barTexture, Texture.class).get(), RectUtility.createFromMinAndSize(minX + 1, minY + 1, barLength, barHeight));
    }

    @ReceiveEvent
    public void getItemTooltipDamageAdjacent(GetItemTooltip event, EntityRef entityRef, ToolDamageAdjacentComponent damageAdjacentComponent) {
        event.getTooltipLines().add(new TooltipLine("Damages " + damageAdjacentComponent.directions.size() + " adjacent blocks"));
    }

    @ReceiveEvent
    public void getItemTooltipBlockPlacement(GetItemTooltip event, EntityRef entityRef, ToolBlockPlacementComponent blockPlacementComponent) {
        event.getTooltipLines().add(new TooltipLine("Places blocks from a block placer ammo chest"));
    }

    @Override
    public void renderOpaque() {

    }

    @Override
    public void renderAlphaBlend() {

    }

    @Override
    public void renderOverlay() {
        SelectedInventorySlotComponent selectedInventorySlotComponent = localPlayer.getCharacterEntity().getComponent(SelectedInventorySlotComponent.class);
        if (selectedInventorySlotComponent != null) {
            EntityRef selectedItem = inventoryManager.getItemInSlot(localPlayer.getCharacterEntity(), selectedInventorySlotComponent.slot);
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

    }

    @Override
    public void renderShadows() {

    }
}
