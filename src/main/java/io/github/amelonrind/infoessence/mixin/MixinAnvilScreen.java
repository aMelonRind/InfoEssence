package io.github.amelonrind.infoessence.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.amelonrind.infoessence.item.ItemStat;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(AnvilScreen.class)
public abstract class MixinAnvilScreen {

    @ModifyArg(method = "onSlotUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/widget/TextFieldWidget;setText(Ljava/lang/String;)V"))
    private String onGetName(String text, @Local(argsOnly = true) ItemStack stack) {
        return ItemStat.getRealName(stack, text);
    }

}
