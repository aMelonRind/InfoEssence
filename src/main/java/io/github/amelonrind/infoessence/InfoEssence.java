package io.github.amelonrind.infoessence;

import io.github.amelonrind.infoessence.config.Config;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class InfoEssence implements ClientModInitializer {
    public static final MinecraftClient mc = MinecraftClient.getInstance();
    public static final String MOD_ID = "meloutils";
    public static final Logger LOGGER = LogManager.getLogger(InfoEssence.class);

    @Override
    public void onInitializeClient() {
        Config.HANDLER.load();
        Config.get().fixValues();
    }

    @Contract(value = "_ -> new", pure = true)
    public static @NotNull MutableText translatable(String key) {
        return Text.translatable(MOD_ID + "." + key);
    }

    public static @NotNull MutableText translatable(String key, Object ...args) {
        return Text.translatable(MOD_ID + "." + key, args);
    }

}
