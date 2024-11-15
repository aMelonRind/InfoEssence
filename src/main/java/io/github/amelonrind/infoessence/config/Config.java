package io.github.amelonrind.infoessence.config;

import com.google.common.collect.ImmutableMap;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import io.github.amelonrind.infoessence.InfoEssence;
import io.github.amelonrind.infoessence.item.ItemStat.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Config {
    public static final ConfigClassHandler<Config> HANDLER = ConfigClassHandler.createBuilder(Config.class)
            .id(Identifier.of(InfoEssence.MOD_ID, "main"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve(InfoEssence.MOD_ID).resolve("settings.json5"))
                    .setJson5(true)
                    .build())
            .build();

    public static Config get() {
        return HANDLER.instance();
    }

    @SerialEntry
    public boolean enabled = true;

    @SerialEntry(comment = "Whether to display overall percentage in item name.")
    public boolean displayInName = true;

    @SerialEntry(comment = "Whether to display individual percentage in lore.")
    public boolean displayInLore = true;

    @SerialEntry(comment = "Whether to check refines.")
    public boolean checkRefines = true;

    @SerialEntry(comment = "The stat multiplier of each refine.")
    public double refineMultiplier = 1.1;

    @SerialEntry(comment = "The regex for each stats. Required named groups: name, value.")
    public String statRegex = "^ ?. (?<name>.+): \\+?(?<value>-?\\d+(?:\\.\\d+)?)%?$";

    @SerialEntry(comment = "The MMO id map to fetch more accurate value, with a multiplier.")
    public Map<String, MmoStatFetcher> mmoMap = new ImmutableMap.Builder<String, MmoStatFetcher>()
            .put("傷害加成", new MmoStatFetcher("PHYSICAL_DAMAGE"))
            .put("暴擊機率", new MmoStatFetcher("CRITICAL_STRIKE_CHANCE"))
            .put("暴擊傷害", new MmoStatFetcher("CRITICAL_STRIKE_POWER"))
            .put("移動速度", new MmoStatFetcher("MOVEMENT_SPEED", 100))
            .put("最大生命", new MmoStatFetcher("MAX_HEALTH"))
            .put("最大魔力", new MmoStatFetcher("MAX_MANA"))
            .put("血量回復", new MmoStatFetcher("HEALTH_REGENERATION"))
            .put("魔力回復", new MmoStatFetcher("MANA_REGENERATION"))

            .put("攻擊傷害", new MmoStatFetcher("ATTACK_DAMAGE"))
            .put("防禦力", new MmoStatFetcher("DEFENSE"))
            .put("物理傷害減免", new MmoStatFetcher("PHYSICAL_DAMAGE_REDUCTION"))
            .put("格擋機率", new MmoStatFetcher("BLOCK_RATING"))
            .build();

    @SerialEntry(comment = "The table for ranges, structured as Map<mmoItemId, Map<attrName, Range(double, double)>>, " +
            "this has higher priority than rangeRegex.")
    public Map<String, Map<String, Range>> rangeTable = Map.of(
            "OBSIDIAN_SHIELD", Map.of(
                    "攻擊傷害", Range.minmax(4.9, 7.1),
                    "防禦力", Range.minmax(8, 12),
                    "物理傷害減免", Range.minmax(14, 26),
                    "格擋機率", Range.minmax(16.2, 23.8)
            )
    );

    @SerialEntry(comment = "The regex for stat ranges. Required named groups: name, min, max.")
    public String rangeRegex = "^(?<name>.+): (?<min>-?\\d+(?:\\.\\d+)?) -> (?<max>-?\\d+(?:\\.\\d+)?)$";

    @SerialEntry(comment = "A name table for naming mistakes (range name -> stat name)")
    public Map<String, String> nameCorrections = new HashMap<>();

    @SerialEntry(comment = "Whether to display in the lore if there's only 1 possible attribute.")
    public boolean displayInLoreWhenSingle = true;

    @SuppressWarnings("DataFlowIssue")
    @SerialEntry(comment = "List of rarities that sorts from low to high.")
    public List<Rarity> rarities = List.of(
            new Rarity(10, Formatting.RED.getColorValue()),
            new Rarity(20, Formatting.RED.getColorValue()),
            new Rarity(60, Formatting.GOLD.getColorValue()),
            new Rarity(90, Formatting.YELLOW.getColorValue()),
            new Rarity(99, Formatting.GREEN.getColorValue()),
            new Rarity(200, Formatting.LIGHT_PURPLE.getColorValue())
    );

    @SerialEntry(comment = "The position of the MMOITEMS_ITEM_ID display in lore.")
    public MmoIdDisplayPosition idDisplayPosition = MmoIdDisplayPosition.BOTTOM;

}
