package io.github.amelonrind.infoessence.item;

import com.google.common.collect.Lists;
import com.google.gson.annotations.SerializedName;
import io.github.amelonrind.infoessence.InfoEssence;
import io.github.amelonrind.infoessence.config.Config;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.AbstractNbtNumber;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.MathHelper;
import org.apache.commons.lang3.function.Consumers;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ItemStat {
    private static final Style NO_ITALIC = Style.EMPTY.withItalic(false);
    private static final Set<ItemStack> checked = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Map<ItemStack, String> parsed = new WeakHashMap<>();
    private static final Set<ItemStack> warned = Collections.newSetFromMap(new WeakHashMap<>());
    private static Consumer<ItemStack> parser = Consumers.nop();

    private ItemStat() {}

    public static void process(ItemStack stack) {
//        if (!Config.get().enabled) return;
        synchronized (checked) {
            if (checked.add(stack)) {
                try {
                    parser.accept(stack);
                } catch (Throwable e) {
                    InfoEssence.LOGGER.error(e);
                }
            }
        }
    }

    public static String getRealName(ItemStack stack, String def) {
        return parsed.getOrDefault(stack, def);
    }

    public static void generateParser(Config cfg) throws MissingGroupException {
        synchronized (checked) {
            try {
                boolean displayInName = cfg.displayInName;
                boolean displayInLore = cfg.displayInLore;
                if (!displayInName && !displayInLore) {
                    parser = Consumers.nop();
                    return;
                }
                boolean checkRefines = cfg.checkRefines;
                double refineMultiplier = cfg.refineMultiplier;
                Pattern statRegex = Pattern.compile(cfg.statRegex);
                {
                    Map<String, Integer> groups = statRegex.namedGroups();
                    if (!groups.containsKey("name") || !groups.containsKey("value")) {
                        throw new MissingGroupException("The statRegex requires two named groups: name, value.");
                    }
                }
                boolean checkMmo = !cfg.mmoMap.isEmpty();
                Map<String, MmoStatFetcher> mmoMap = cfg.mmoMap;

                // Map<mmoItemId, Map<attrName, Range>>
                Map<String, Map<String, Range>> rangeTable = cfg.rangeTable;
                boolean useRangeRegex = !cfg.rangeRegex.isBlank();
                Pattern rangeRegex = useRangeRegex ? Pattern.compile(cfg.rangeRegex) : null;
                if (useRangeRegex) {
                    Map<String, Integer> groups = rangeRegex.namedGroups();
                    if (!groups.containsKey("name") || !groups.containsKey("min") || !groups.containsKey("max")) {
                        throw new MissingGroupException("The rangeRegex requires three named groups: name, min, max.");
                    }
                }
                Map<String, String> nameCorrections = cfg.nameCorrections;
                boolean displayInLoreWhenSingle = cfg.displayInLoreWhenSingle;
                Function<Float, Integer> rarities = parseRarities(cfg.rarities);
                MmoIdDisplayPosition idDisplayPosition = cfg.idDisplayPosition;

                Function<MmoItemHelper, Double> multiplierGetter = checkRefines
                        ? item -> 1.0 / Math.pow(refineMultiplier, item.getRefines())
                        : item -> 1.0;

                Function<ItemStack, MmoItemHelper> mmoHelperGetter =
                        checkRefines || checkMmo || !rangeTable.isEmpty() || idDisplayPosition != MmoIdDisplayPosition.NONE
                        ? MmoItemHelper::new
                        : item -> null;

                BiConsumer<MmoItemHelper, Map<String, ParsedStat>> mmoStatFetcher = checkMmo
                        ? (item, stats) -> {
                            for (ParsedStat stat : stats.values()) {
                                MmoStatFetcher fetcher = mmoMap.get(stat.name);
                                if (fetcher != null) fetcher.fetch(item, stat);
                            }
                        }
                        : (item, stats) -> {};

                Function<String, String> nameCorrector = nameCorrections.isEmpty()
                        ? Function.identity()
                        : name -> nameCorrections.getOrDefault(name, name);

                BiConsumer<Supplier<String>, Map<String, ParsedStat>> rangeTableApplier = rangeTable.isEmpty()
                        ? (mmoIdSupplier, stats) -> {}
                        : (mmoIdSupplier, stats) -> {
                            Map<String, Range> ranges = rangeTable.get(mmoIdSupplier.get());
                            if (ranges == null) return;
                            for (ParsedStat stat : stats.values()) {
                                stat.assignRange(ranges.get(stat.name));
                            }
                        };

                BiConsumer<List<String>, Map<String, ParsedStat>> rangeRegexApplier = useRangeRegex
                        ? (lore, stats) -> {
                            for (int i = 0; i < lore.size(); i++) {
                                Matcher match = rangeRegex.matcher(lore.get(i));
                                if (!match.matches()) continue;
                                String name = match.group("name");
                                ParsedStat stat = stats.get(nameCorrector.apply(name));
                                if (stat != null && stat.assignRange(match)) {
                                    stat.index = i;
                                }
                            }
                        }
                        : (lore, stats) -> {};

                Function<Float, Text> percentageTagGetter = p -> {
                    boolean high = p > 97;
                    float precision = high ? 100.0f : 10.0f;
                    float rounded = (float) Math.floor(p * precision) / precision;
                    MutableText tag = InfoEssence.translatable(
                            high ? "tooltip.percentageTag.high" : "tooltip.percentageTag",
                            rounded
                    );
                    int sortHint = MathHelper.clamp((int) Math.floor(Math.log10(rounded)), 0, 9);
                    return Text.empty()
                            .append("§" + sortHint)
                            .append(tag.setStyle(NO_ITALIC).withColor(rarities.apply(rounded)));
                };


                parser = stack -> {
                    LoreComponent loreC = stack.get(DataComponentTypes.LORE);
                    if (loreC == null) return;
                    List<Text> lore = loreC.lines();
                    List<String> loreStr = Lists.transform(lore, Text::getString);
                    Map<String, ParsedStat> stats = new HashMap<>();
                    for (int i = 0; i < loreStr.size(); i++) {
                        String line = loreStr.get(i);
                        Matcher match = statRegex.matcher(line);
                        if (!match.matches()) continue;
                        String name = match.group("name");
                        stats.put(name, new ParsedStat(name, Double.parseDouble(match.group("value")), i));
                    }
                    MmoItemHelper mmo = mmoHelperGetter.apply(stack);
                    mmoStatFetcher.accept(mmo, stats);
                    rangeTableApplier.accept(mmo::getId, stats);
                    rangeRegexApplier.accept(loreStr, stats);
                    double multiplier = multiplierGetter.apply(mmo);
                    List<ParsedStat> computedStats = stats.values().stream().filter(stat -> stat.calculate(multiplier)).toList();
                    if (computedStats.isEmpty()) return;

                    Text name = stack.getName();
                    parsed.put(stack, name.getString());

                    if (displayInLore && (displayInLoreWhenSingle || computedStats.size() > 1)) {
                        List<Text> newLore = new ArrayList<>(lore);
                        for (ParsedStat stat : computedStats) {
                            newLore.set(stat.index, Text.empty()
                                    .append(newLore.get(stat.index))
                                    .append(percentageTagGetter.apply(stat.percentage))
                            );
                        }
                        idDisplayPosition.action.accept(newLore, mmo::getId);
                        stack.set(DataComponentTypes.LORE, new LoreComponent(newLore));
                    } else if (idDisplayPosition != MmoIdDisplayPosition.NONE) {
                        List<Text> newLore = new ArrayList<>(lore);
                        idDisplayPosition.action.accept(newLore, mmo::getId);
                        stack.set(DataComponentTypes.LORE, new LoreComponent(newLore));
                    }
                    if (displayInName) {
                        OptionalDouble avg = computedStats.stream().mapToDouble(s -> s.percentage).average();
                        if (avg.isPresent()) {
                            stack.set(DataComponentTypes.CUSTOM_NAME, Text.empty()
                                    .append(name)
                                    .append(percentageTagGetter.apply((float) avg.getAsDouble())
                            ));
                        }
                    }
                };
            } catch (Throwable e) {
                parser = Consumers.nop();
                throw e;
            } finally {
                Text warn = InfoEssence.translatable("tooltip.updateNeeded").setStyle(NO_ITALIC).formatted(Formatting.DARK_GRAY);
                for (ItemStack stack : parsed.keySet()) {
                    if (warned.add(stack)) {
                        LoreComponent lore = stack.get(DataComponentTypes.LORE);
                        if (lore == null) continue;
                        List<Text> list = new ArrayList<>();
                        list.add(warn);
                        list.addAll(lore.lines());
                        stack.set(DataComponentTypes.LORE, new LoreComponent(list));
                    }
                }
            }
        }
    }

    private static @NotNull Function<Float, Integer> parseRarities(@NotNull List<Rarity> rarities) {
        if (rarities.isEmpty()) return p -> 0xFFFFFF;
        List<Rarity> list = new ArrayList<>(rarities);
        list.sort((a, b) -> Float.compare(a.ceilPercentage, b.ceilPercentage));
        Rarity last = list.getFirst();
        for (int i = 1; i < list.size();) {
            Rarity r = list.get(i);
            boolean remove = last.color == r.color;
            last = r;
            if (remove) {
                list.remove(i - 1);
            } else {
                i++;
            }
        }
        return switch (list.size()) {
            case 1 -> {
                int v = list.getFirst().color;
                yield p -> v;
            }
            case 2 -> {
                Rarity first = list.getFirst();
                float ceil = (float) Math.floor(first.ceilPercentage * 100.0f) / 100.0f;
                int a = first.color;
                int b = list.getLast().color;
                yield p -> p < ceil ? a : b;
            }
            default -> {
                int size = list.size();
                float[] ceilThresholds = new float[size];
                int[] colors = new int[size];
                for (int i = 0; i < size; i++) {
                    Rarity r = list.get(i);
                    ceilThresholds[i] = (float) Math.floor(r.ceilPercentage * 100.0f) / 100.0f;
                    colors[i] = r.color;
                }
                yield p -> {
                    int left = 0;
                    int right = size - 1;
                    while (left != right) {
                        int center = (right + left) / 2;
                        if (p < ceilThresholds[center]) {
                            right = center;
                        } else {
                            left = center + 1;
                        }
                    }
                    return colors[right];
                };
            }
        };
    }

    private static final class ParsedStat {
        final String name;
        double value;
        int index;
        boolean hasRange = false;
        double min = 0.0;
        double range = 0.0;
        boolean display = false;
        float percentage = 0.0f;

        private ParsedStat(String name, double value, int index) {
            this.name = name;
            this.value = value;
            this.index = index;
        }

        void assignRange(@Nullable Range range) {
            if (!hasRange && range != null) {
                hasRange = true;
                min = range.min;
                this.range = range.range;
            }
        }

        boolean assignRange(Matcher match) {
            if (!hasRange) {
                hasRange = true;
                min = Double.parseDouble(match.group("min"));
                range = Double.parseDouble(match.group("max")) - min;
                return true;
            }
            return false;
        }

        boolean calculate(double multiplier) {
            if (hasRange && range != 0) {
                display = true;
                percentage = (float) ((value * multiplier - min) / range) * 100.0f + 0.0001f;
                return true;
            }
            return false;
        }
    }

    public record Range(double min, double range) {

        @Contract("_, _ -> new")
        public static @NotNull Range minmax(double min, double max) {
            return new Range(min, max - min);
        }

    }

    public record MmoStatFetcher(String key, double multiplier) {

        public MmoStatFetcher(String key) {
            this(key, 1.0);
        }

        void fetch(@NotNull MmoItemHelper item, ParsedStat stat) {
            if (item.nbt.get(key) instanceof AbstractNbtNumber ann) {
                stat.value = ann.doubleValue() * multiplier;
            }
        }
    }

    public record Rarity(float ceilPercentage, int color) {}

    public enum MmoIdDisplayPosition {
        @SerializedName("NONE")
        NONE((list, text) -> {}),
        @SerializedName("TOP")
        TOP((texts, e) -> texts.addFirst(InfoEssence.translatable("tooltip.mmoItemId", e.get()).setStyle(NO_ITALIC).formatted(Formatting.GRAY))),
        @SerializedName("BOTTOM")
        BOTTOM((texts, e) -> texts.addLast(InfoEssence.translatable("tooltip.mmoItemId", e.get()).setStyle(NO_ITALIC).formatted(Formatting.GRAY)));

        public final BiConsumer<List<Text>, Supplier<String>> action;

        MmoIdDisplayPosition(BiConsumer<List<Text>, Supplier<String>> action) {
            this.action = action;
        }

    }

    public static class MissingGroupException extends Exception {
        public MissingGroupException(String message) {
            super(message);
        }
    }

}
