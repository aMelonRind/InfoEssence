package io.github.amelonrind.infoessence.mixin;

import io.github.amelonrind.infoessence.config.Config;
import io.github.amelonrind.infoessence.item.ItemStat;
import net.minecraft.component.ComponentHolder;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

// unused, target is interface.
@Mixin(ComponentHolder.class)
public class MixinComponentHolder {
    @Unique
    private static final Set<ComponentType<?>> triggeringTypes = Set.of(
            DataComponentTypes.CUSTOM_NAME,
            DataComponentTypes.ITEM_NAME,
            DataComponentTypes.LORE
    );

    @Inject(method = "get", at = @At("HEAD"))
    private <T> void onGet(ComponentType<? extends T> type, CallbackInfoReturnable<T> cir) {
        if (Config.get().enabled && triggeringTypes.contains(type) && ((Object) this).getClass() == ItemStack.class) {
            //noinspection DataFlowIssue
            ItemStat.process((ItemStack) (Object) this);
        }
    }

}
