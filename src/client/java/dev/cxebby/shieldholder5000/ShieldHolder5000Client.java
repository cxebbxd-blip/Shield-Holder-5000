package dev.cxebby.shieldholder5000;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;

public final class ShieldHolder5000Client implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
            Identifier.fromNamespaceAndPath("shield_holder_5000", "controls")
    );

    private static final KeyMapping TOGGLE_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping(
                    "key.shield_holder_5000.toggle",
                    InputConstants.Type.KEYSYM,
                    InputConstants.KEY_H,
                    CATEGORY
            )
    );

    private static boolean enabled;
    private static boolean savedPauseOnLostFocus;
    private static boolean savedPauseValueValid;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("shieldhold")
                        .executes(context -> {
                            setEnabled(Minecraft.getInstance(), !enabled);
                            return 1;
                        })
                        .then(ClientCommandManager.literal("on").executes(context -> {
                            setEnabled(Minecraft.getInstance(), true);
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("off").executes(context -> {
                            setEnabled(Minecraft.getInstance(), false);
                            return 1;
                        }))
                        .then(ClientCommandManager.literal("status").executes(context -> {
                            sendStatus(Minecraft.getInstance());
                            return 1;
                        }))
                )
        );

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            while (TOGGLE_KEY.consumeClick()) {
                setEnabled(client, !enabled);
            }

            if (!enabled || client.player == null || client.gameMode == null) {
                return;
            }

            client.options.pauseOnLostFocus = false;

            InteractionHand shieldHand = findShieldHand(client);
            if (shieldHand == null) {
                return;
            }

            boolean alreadyUsingShield = client.player.isUsingItem()
                    && client.player.getUseItem().is(Items.SHIELD)
                    && client.player.getUsedItemHand() == shieldHand;

            if (!alreadyUsingShield) {
                if (client.player.isUsingItem()) {
                    client.gameMode.releaseUsingItem(client.player);
                }
                client.gameMode.useItem(client.player, shieldHand);
            }
        });
    }

    private static InteractionHand findShieldHand(Minecraft client) {
        if (client.player.getOffhandItem().is(Items.SHIELD)) {
            return InteractionHand.OFF_HAND;
        }
        if (client.player.getMainHandItem().is(Items.SHIELD)) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private static void setEnabled(Minecraft client, boolean value) {
        if (enabled == value) {
            sendStatus(client);
            return;
        }

        enabled = value;

        if (enabled) {
            savedPauseOnLostFocus = client.options.pauseOnLostFocus;
            savedPauseValueValid = true;
            client.options.pauseOnLostFocus = false;
        } else if (savedPauseValueValid) {
            client.options.pauseOnLostFocus = savedPauseOnLostFocus;
            savedPauseValueValid = false;

            if (client.player != null && client.gameMode != null
                    && client.player.isUsingItem()
                    && client.player.getUseItem().is(Items.SHIELD)) {
                client.gameMode.releaseUsingItem(client.player);
            }
        }

        sendStatus(client);
    }

    private static void sendStatus(Minecraft client) {
        if (client.player != null) {
            client.player.displayClientMessage(
                    Component.literal("Shield Hold: " + (enabled ? "ON" : "OFF")),
                    true
            );
        }
    }
}
