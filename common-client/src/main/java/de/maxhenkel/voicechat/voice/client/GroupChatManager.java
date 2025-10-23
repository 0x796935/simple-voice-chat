package de.maxhenkel.voicechat.voice.client;

import de.maxhenkel.voicechat.Voicechat;
import de.maxhenkel.voicechat.VoicechatClient;
import de.maxhenkel.voicechat.gui.GameProfileUtils;
import de.maxhenkel.voicechat.voice.common.PlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class GroupChatManager {

    private static final ResourceLocation TALK_OUTLINE = ResourceLocation.fromNamespaceAndPath(Voicechat.MODID, "textures/icons/talk_outline.png");
    private static final ResourceLocation SPEAKER_OFF_ICON = ResourceLocation.fromNamespaceAndPath(Voicechat.MODID, "textures/icons/speaker_small_off.png");

    public static void renderIcons(GuiGraphics guiGraphics) {
        ClientVoicechat client = ClientManager.getClient();

        if (client == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();

        List<PlayerState> groupMembers = getGroupMembers(VoicechatClient.CLIENT_CONFIG.showOwnGroupIcon.get());

        guiGraphics.pose().pushMatrix();
        int posX = VoicechatClient.CLIENT_CONFIG.groupPlayerIconPosX.get();
        int posY = VoicechatClient.CLIENT_CONFIG.groupPlayerIconPosY.get();
        if (posX < 0) {
            guiGraphics.pose().translate(mc.getWindow().getGuiScaledWidth(), 0F);
        }
        if (posY < 0) {
            guiGraphics.pose().translate(0F, mc.getWindow().getGuiScaledHeight());
        }
        guiGraphics.pose().translate(posX, posY);

        float scale = VoicechatClient.CLIENT_CONFIG.groupHudIconScale.get().floatValue();
        guiGraphics.pose().scale(scale, scale);

        boolean vertical = VoicechatClient.CLIENT_CONFIG.groupPlayerIconOrientation.get().equals(GroupPlayerIconOrientation.VERTICAL);

        for (int i = 0; i < groupMembers.size(); i++) {
            PlayerState state = groupMembers.get(i);
            guiGraphics.pose().pushMatrix();
            if (vertical) {
                if (posY < 0) {
                    guiGraphics.pose().translate(0F, i * -11F);
                } else {
                    guiGraphics.pose().translate(0F, i * 11F);
                }
            } else {
                if (posX < 0) {
                    guiGraphics.pose().translate(i * -11F, 0F);
                } else {
                    guiGraphics.pose().translate(i * 11F, 0F);
                }
            }

            if (client.getTalkCache().isTalking(state.getUuid())) {
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, TALK_OUTLINE, posX < 0 ? -10 : 0, posY < 0 ? -10 : 0, 0, 0, 10, 10, 16, 16);
            }
            PlayerSkin skin = GameProfileUtils.getSkin(state.getUuid());
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), posX < 0 ? -1 - 8 : 1, posY < 0 ? -1 - 8 : 1, 8, 8, 8, 8, 64, 64);
            guiGraphics.blit(RenderPipelines.GUI_TEXTURED, skin.body().texturePath(), posX < 0 ? -1 - 8 : 1, posY < 0 ? -1 - 8 : 1, 40, 8, 8, 8, 64, 64);

            if (state.isDisabled()) {
                guiGraphics.pose().pushMatrix();
                guiGraphics.pose().translate((posX < 0 ? -1F - 8F : 1F), posY < 0 ? -1F - 8F : 1F);
                guiGraphics.pose().scale(0.5F, 0.5F);
                guiGraphics.blit(RenderPipelines.GUI_TEXTURED, SPEAKER_OFF_ICON, 0, 0, 0, 0, 16, 16, 16, 16);
                guiGraphics.pose().popMatrix();
            }

            // Add player name rendering next to head (half-size text)
            String playerName = state.getName();
            if (playerName != null && !playerName.isEmpty()) {
                // Calculate name position based on orientation
                int nameX, nameY;
                if (vertical) {
                    nameX = posX < 0 ? -11 : 11; // Left or right of head
                    nameY = posY < 0 ? -5 : 1;  // Vertically centered
                } else {
                    nameX = posX < 0 ? -8 - mc.font.width(playerName) : 11; // Left or right of head
                    nameY = posY < 0 ? -5 - mc.font.lineHeight : 1; // Above or below head
                }

                // Prepare scaled sizes (we will draw the background/border in unscaled coordinates sized for the scaled text)
                float textScale = 0.25F;
                int textWidth = mc.font.width(playerName);
                int textHeight = mc.font.lineHeight;
                int drawWidth = Math.max(1, Math.round(textWidth * textScale));
                int drawHeight = Math.max(1, Math.round(textHeight * textScale));

                // If player is talking, add highlight border around name matching the head outline
                if (client.getTalkCache().isTalking(state.getUuid())) {
                    // Semi-transparent background for better readability (sized for scaled text)
                    guiGraphics.fill(nameX - 2, nameY - 2, nameX + drawWidth + 2, nameY + drawHeight + 2, 0x80000000);

                    // Highlight border - matching the style of TALK_OUTLINE (white)
                    int borderColor = 0xFFFFFFFF;
                    // Top border
                    guiGraphics.fill(nameX - 2, nameY - 2, nameX + drawWidth + 2, nameY - 1, borderColor);
                    // Bottom border
                    guiGraphics.fill(nameX - 2, nameY + drawHeight + 1, nameX + drawWidth + 2, nameY + drawHeight + 2, borderColor);
                    // Left border
                    guiGraphics.fill(nameX - 2, nameY - 1, nameX - 1, nameY + drawHeight + 1, borderColor);
                    // Right border
                    guiGraphics.fill(nameX + drawWidth + 1, nameY - 1, nameX + drawWidth + 2, nameY + drawHeight + 1, borderColor);
                }

                // Draw the player name at half size using pose scaling
                guiGraphics.pose().pushMatrix();
                // Translate to name position and scale down
                guiGraphics.pose().translate(nameX, nameY, 0F);
                guiGraphics.pose().scale(textScale, textScale, 1F);
                // Draw at 0,0 because we've translated to nameX/nameY already
                guiGraphics.drawString(mc.font, playerName, 0, 0, 0xFFFFFFFF);
                guiGraphics.pose().popMatrix();
            }

            guiGraphics.pose().popMatrix();
        }
        guiGraphics.pose().popMatrix();
    }



    public static List<PlayerState> getGroupMembers() {
        return getGroupMembers(true);
    }

    public static List<PlayerState> getGroupMembers(boolean includeSelf) {
        List<PlayerState> entries = new ArrayList<>();
        UUID group = ClientManager.getPlayerStateManager().getGroupID();

        if (group == null) {
            return entries;
        }

        for (PlayerState state : ClientManager.getPlayerStateManager().getPlayerStates(includeSelf)) {
            if (state.hasGroup() && state.getGroup().equals(group)) {
                entries.add(state);
            }
        }

        entries.sort(Comparator.comparing(PlayerState::getName));

        return entries;
    }

}
