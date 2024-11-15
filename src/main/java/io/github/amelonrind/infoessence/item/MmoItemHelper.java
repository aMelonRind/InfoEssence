package io.github.amelonrind.infoessence.item;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("unused")
public class MmoItemHelper {
    public final ItemStack base;
    public final NbtCompound nbt;

    public MmoItemHelper(ItemStack stack) {
        base = stack;
        NbtComponent comp = base.get(DataComponentTypes.CUSTOM_DATA);
        nbt = comp != null ? comp.copyNbt() : new NbtCompound();
    }

    @Nullable
    private NbtElement getMmoNbt(@NotNull String key) {
        return nbt.get(key.startsWith("HSTRY_") ? key : "MMOITEMS_" + key);
    }

    @Nullable
    public String getId() {
        return getString("ITEM_ID");
    }

    @Nullable
    // number | string | null
    public Object get(String key) {
        NbtElement e = getMmoNbt(key);
        if (e instanceof AbstractNbtNumber ann) return ann.doubleValue();
        else if (e instanceof NbtString ns) return ns.asString();
        return null;
    }

    @Nullable
    public Double getNumber(String key) {
        if (getMmoNbt(key) instanceof AbstractNbtNumber ann) return ann.doubleValue();
        else return null;
    }

    @Nullable
    public String getString(String key) {
        if (getMmoNbt(key) instanceof NbtString ann) return ann.asString();
        else return null;
    }

    @Nullable
    public JsonObject getJson(String key) {
        String str = getString(key);
        if (str == null) return null;
        try {
            return JsonParser.parseString(str).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException ignore) {}
        return null;
    }

    @Nullable
    public Double getDblOGStory(String key) {
        JsonObject o = getJson("HSTRY_" + key);
        if (o == null) return null;
        // sample: {"Stat":"PHYSICAL_DAMAGE","OGStory":[{"MMOITEMS_PHYSICAL_DAMAGE_ñdbl":15.9014}]}
        try {
            return o.getAsJsonArray("OGStory")
                    .get(0)
                    .getAsJsonObject()
                    .get("MMOITEMS_" + key + "_ñdbl")
                    .getAsDouble();
        } catch (Throwable ignore) {}
        return null;
    }

    public int getRefines() {
        JsonObject o = getJson("UPGRADE");
        if (o == null) return 0;
        // sample: {"Template":"default","Workbench":false,"Destroy":false,"Level":15,"Max":15,"Min":0,"Success":0.0}
        try {
            return o.get("Level").getAsInt();
        } catch (Throwable ignore) {}
        return 0;
    }

}
