package io.github.amelonrind.infoessence.config;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import dev.isxander.yacl3.gui.controllers.string.StringController;
import io.github.amelonrind.infoessence.InfoEssence;
import io.github.amelonrind.infoessence.item.ItemStat.*;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class ModMenuApiImpl implements ModMenuApi {
    private static final Gson GSON = new Gson();
    private static final Type MMO_MAP_TYPE = TypeToken.getParameterized(Map.class, String.class, MmoStatFetcher.class).getType(); // Map<String, MmoStatFetcher>
    private static final Type RANGE_TABLE_TYPE = TypeToken.getParameterized(Map.class, String.class, TypeToken.getParameterized(Map.class, String.class, Range.class).getType()).getType(); // Map<String, Map<String, Range>>
    private static final Type NAME_CORRECTIONS_TYPE = TypeToken.getParameterized(Map.class, String.class, String.class).getType(); // Map<String, String>
    private static final Type RARITIES_TYPE = TypeToken.getParameterized(List.class, Rarity.class).getType(); // List<Rarity>

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull MutableText translatable(String key) {
        return InfoEssence.translatable("settings." + key);
    }

    @Contract(value = "_ -> new", pure = true)
    private static @NotNull OptionDescription descriptionOf(String key) {
        return OptionDescription.createBuilder()
                .text(translatable(key + ".description"))
                .build();
    }

    private static <T> Option.Builder<T> createBuilder(String name) {
        return Option.<T>createBuilder().name(translatable(name)).description(descriptionOf(name));
    }

    private static Option<Boolean> optionOf(String name, boolean def, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return ModMenuApiImpl.<Boolean>createBuilder(name)
                .binding(def, getter, setter)
                .controller(TickBoxControllerBuilder::create)
                .build();
    }

    private static Option<Boolean> optionOf(String name, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return optionOf(name, true, getter, setter);
    }

    private static <T> Option<String> optionOfJson(String name, Type type, T def, Supplier<T> getter, Consumer<T> setter) {
        return Option.<String>createBuilder()
                .name(translatable(name))
                .description(OptionDescription.createBuilder()
                        .text(
                                translatable(name + ".description"),
                                Text.empty(),
                                translatable("externalEditorRecommended")
                        )
                        .build())
                .binding(GSON.toJson(def), () -> GSON.toJson(getter.get()), str -> {
                    try {
                        T o = GSON.fromJson(str, type);
                        if (o != null) setter.accept(o);
                    } catch (Throwable ignored) {}
                })
                .customController(ValidatedJsonStringController.of(type))
                .build();
    }

    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return p -> {
            Config def = new Config();
            Config cfg = Config.get();
            return YetAnotherConfigLib.createBuilder()
                    .title(translatable("title"))
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("category.general"))
                            .option(optionOf("enabled", () -> cfg.enabled, val -> cfg.enabled = val))
                            .build())
                    .category(ConfigCategory.createBuilder()
                            .name(translatable("category.parser"))
                            .option(optionOf("checkMmoIdFirst", () -> cfg.checkMmoIdFirst, val -> cfg.checkMmoIdFirst = val))
                            .option(optionOf("displayInName", () -> cfg.displayInName, val -> cfg.displayInName = val))
                            .option(optionOf("displayInLore", () -> cfg.displayInLore, val -> cfg.displayInLore = val))
                            .option(optionOf("checkRefines", () -> cfg.checkRefines, val -> cfg.checkRefines = val))
                            .option(ModMenuApiImpl.<Double>createBuilder("refineMultiplier")
                                    .binding(def.refineMultiplier, () -> cfg.refineMultiplier, val -> cfg.refineMultiplier = val)
                                    .controller(DoubleFieldControllerBuilder::create)
                                    .build())
                            .option(ModMenuApiImpl.<String>createBuilder("statRegex")
                                    .binding(def.statRegex, () -> cfg.statRegex, val -> cfg.statRegex = val)
                                    .controller(StringControllerBuilder::create)
                                    .build())
                            .option(optionOfJson("mmoMap", MMO_MAP_TYPE, def.mmoMap, () -> cfg.mmoMap, val -> cfg.mmoMap = val))
                            .option(optionOfJson("rangeTable", RANGE_TABLE_TYPE, def.rangeTable, () -> cfg.rangeTable, val -> cfg.rangeTable = val))
                            .option(ModMenuApiImpl.<String>createBuilder("rangeRegex")
                                    .binding(def.rangeRegex, () -> cfg.rangeRegex, val -> cfg.rangeRegex = val)
                                    .controller(StringControllerBuilder::create)
                                    .build())
                            .option(optionOfJson("nameCorrections", NAME_CORRECTIONS_TYPE, def.nameCorrections, () -> cfg.nameCorrections, val -> cfg.nameCorrections = val))
                            .option(optionOf("displayInLoreWhenSingle", () -> cfg.displayInLoreWhenSingle, val -> cfg.displayInLoreWhenSingle = val))
                            .option(optionOfJson("rarities", RARITIES_TYPE, def.rarities, () -> cfg.rarities, val -> cfg.rarities = val))
                            .option(ModMenuApiImpl.<MmoIdDisplayPosition>createBuilder("idDisplayPosition")
                                    .binding(def.idDisplayPosition, () -> cfg.idDisplayPosition, val -> cfg.idDisplayPosition = val)
                                    .controller(opt -> EnumControllerBuilder.create(opt)
                                            .enumClass(MmoIdDisplayPosition.class))
                                    .build())
                            .build())
                    .save(() -> {
                        Config.HANDLER.save();
                        InfoEssence.applyConfig();
                    })
                    .build()
                    .generateScreen(p);
        };
    }

    public static class ValidatedJsonStringController extends StringController {
        public final Type type;

        public static Function<Option<String>, Controller<String>> of(Type type) {
            return opt -> new ValidatedJsonStringController(opt, type);
        }

        /**
         * Constructs a validated string controller
         *
         * @param option bound option
         */
        public ValidatedJsonStringController(Option<String> option, Type type) {
            super(option);
            this.type = type;
        }

        @Override
        public boolean isInputValid(String input) {
            try {
                return GSON.fromJson(input, type) != null;
            } catch (Throwable e) {
                return false;
            }
        }

    }

}
