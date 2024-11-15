package io.github.amelonrind.infoessence.item;

import com.google.gson.*;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

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
    public String getType() {
        return getString("ITEM_TYPE");
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

    private Optional<JsonObject> getJson(String key) {
        return Optional.ofNullable(getString(key))
                .map(str -> {
                    try {
                        return JsonParser.parseString(str);
                    } catch (JsonSyntaxException e) {
                        return null;
                    }
                })
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject);
    }

    @Nullable
    public Double getDblOGStory(String key) {
        // sample: {"Stat":"PHYSICAL_DAMAGE","OGStory":[{"MMOITEMS_PHYSICAL_DAMAGE_ñdbl":15.9014}]}
        return getJson("HSTRY_" + key)
                .map(j -> j.get("OGStory"))
                .filter(JsonElement::isJsonArray)
                .map(JsonElement::getAsJsonArray)
                .map(j -> j.get(0))
                .filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject)
                .map(j -> j.get("MMOITEMS_" + key + "_ñdbl"))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isNumber)
                .map(JsonPrimitive::getAsDouble)
                .orElse(null);
    }

    public int getRefines() {
        // sample: {"Template":"default","Workbench":false,"Destroy":false,"Level":15,"Max":15,"Min":0,"Success":0.0}
        return getJson("UPGRADE")
                .map(j -> j.get("Level"))
                .filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsJsonPrimitive)
                .filter(JsonPrimitive::isNumber)
                .map(JsonPrimitive::getAsInt)
                .orElse(0);
    }

}
