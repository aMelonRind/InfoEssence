package io.github.amelonrind.infoessence.mixin;

import io.github.amelonrind.infoessence.item.ItemStat;
import net.fabricmc.fabric.api.item.v1.FabricItemStack;
import net.minecraft.component.ComponentChanges;
import net.minecraft.component.ComponentHolder;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.entry.RegistryEntry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("CommentedOutCode")
@Mixin(ItemStack.class)
public abstract class MixinItemStack implements ComponentHolder, FabricItemStack {
    // only changes partial items, which messes up inventory mods.
//    @Unique
//    private static final Set<ComponentType<?>> triggeringTypes = Set.of(
//            DataComponentTypes.CUSTOM_NAME,
//            DataComponentTypes.ITEM_NAME,
//            DataComponentTypes.LORE
//    );
//
//    @Nullable
//    @Override
//    public <T> T get(ComponentType<? extends T> type) {
//        if (triggeringTypes.contains(type)) {
//            ItemStat.process((ItemStack) (Object) this);
//        }
//        return ComponentHolder.super.get(type);
//    }

    @Inject(method = "<init>(Lnet/minecraft/registry/entry/RegistryEntry;ILnet/minecraft/component/ComponentChanges;)V", at = @At("TAIL"))
    private void onReceive(RegistryEntry<?> item, int count, ComponentChanges changes, CallbackInfo ci) {
        ItemStat.process((ItemStack) (Object) this);
    }

}
