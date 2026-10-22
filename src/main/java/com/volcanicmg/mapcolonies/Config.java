package com.volcanicmg.mapcolonies;

import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Server and client config specs. */
public final class Config {
    public enum Labels { NONE, NAME, NAME_LEVEL }

    public enum BuildingColor { COLONY, TYPE }

    public enum LabelMode { ALWAYS, HOVER, NONE }

    public static final ModConfigSpec SERVER_SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_ALL_COLONIES;
    public static final ModConfigSpec.BooleanValue SHOW_OTHER_COLONIES_BUILDINGS;
    public static final ModConfigSpec.IntValue SYNC_INTERVAL_SECONDS;

    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue SHOW_ON_MINIMAP;
    public static final ModConfigSpec.BooleanValue SHOW_ON_WORLD_MAP;
    public static final ModConfigSpec.BooleanValue OUTLINE_ONLY;
    public static final ModConfigSpec.IntValue FILL_OPACITY_OWN;
    public static final ModConfigSpec.IntValue FILL_OPACITY_OTHER;
    public static final ModConfigSpec.IntValue BORDER_OPACITY_OWN;
    public static final ModConfigSpec.IntValue BORDER_OPACITY_OTHER;
    public static final ModConfigSpec.BooleanValue SHOW_BUILDINGS;
    public static final ModConfigSpec.BooleanValue BUILDINGS_ON_MINIMAP;
    public static final ModConfigSpec.BooleanValue BUILDINGS_ON_WORLD_MAP;
    public static final ModConfigSpec.EnumValue<Labels> BUILDING_LABELS;
    public static final ModConfigSpec.BooleanValue ONLY_MY_COLONIES;
    public static final ModConfigSpec.IntValue MIN_BUILDING_LEVEL;
    public static final ModConfigSpec.EnumValue<BuildingColor> BUILDING_COLOR_MODE;
    public static final ModConfigSpec.IntValue BUILDING_FILL_OPACITY;
    public static final ModConfigSpec.IntValue BUILDING_BORDER_OPACITY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> HIDDEN_BUILDING_TYPES;
    public static final ModConfigSpec.BooleanValue BUILDING_ICONS;
    public static final ModConfigSpec.BooleanValue ICONS_ON_MINIMAP;
    public static final ModConfigSpec.BooleanValue ICONS_ON_WORLD_MAP;
    public static final ModConfigSpec.EnumValue<LabelMode> LABEL_MODE_MINIMAP;
    public static final ModConfigSpec.EnumValue<LabelMode> LABEL_MODE_WORLD_MAP;
    public static final ModConfigSpec.DoubleValue WORLD_MAP_LABEL_MIN_ZOOM;
    public static final ModConfigSpec.DoubleValue ICON_SCALE;
    public static final ModConfigSpec.BooleanValue ICON_BACKGROUND;
    public static final ModConfigSpec.DoubleValue LABEL_SCALE;

    static {
        ModConfigSpec.Builder s = new ModConfigSpec.Builder();
        SHOW_ALL_COLONIES = s.comment("Send colonies the player is not a member of too (drawn dimmer).")
                .define("showAllColonies", true);
        SHOW_OTHER_COLONIES_BUILDINGS = s.comment("Send huts of colonies the player is not a member of.")
                .define("showOtherColoniesBuildings", false);
        SYNC_INTERVAL_SECONDS = s.comment("Seconds between diff syncs.")
                .defineInRange("syncIntervalSeconds", 5, 1, 300);
        SERVER_SPEC = s.build();

        ModConfigSpec.Builder c = new ModConfigSpec.Builder();
        ENABLED = c.define("enabled", true);
        SHOW_ON_MINIMAP = c.define("showOnMinimap", true);
        SHOW_ON_WORLD_MAP = c.define("showOnWorldMap", true);
        OUTLINE_ONLY = c.comment("Draw only colony borders, no fill.").define("outlineOnly", false);
        FILL_OPACITY_OWN = c.comment("Percent.").defineInRange("fillOpacityOwn", 35, 0, 100);
        FILL_OPACITY_OTHER = c.comment("Percent.").defineInRange("fillOpacityOther", 15, 0, 100);
        BORDER_OPACITY_OWN = c.comment("Percent.").defineInRange("borderOpacityOwn", 85, 0, 100);
        BORDER_OPACITY_OTHER = c.comment("Percent.").defineInRange("borderOpacityOther", 50, 0, 100);
        c.push("buildings");
        SHOW_BUILDINGS = c.comment("Master switch for hut footprints.").define("showBuildings", true);
        BUILDINGS_ON_MINIMAP = c.define("buildingsOnMinimap", true);
        BUILDINGS_ON_WORLD_MAP = c.define("buildingsOnWorldMap", true);
        BUILDING_LABELS = c.comment("Label text for huts.").defineEnum("buildingLabels", Labels.NAME_LEVEL);
        ONLY_MY_COLONIES = c.comment("Only show huts of colonies you are a member of.")
                .define("onlyMyColonies", true);
        MIN_BUILDING_LEVEL = c.comment("0 also shows placed but unbuilt huts.")
                .defineInRange("minBuildingLevel", 0, 0, 10);
        BUILDING_COLOR_MODE = c.defineEnum("buildingColorMode", BuildingColor.COLONY);
        BUILDING_FILL_OPACITY = c.comment("Percent.").defineInRange("buildingFillOpacity", 45, 0, 100);
        BUILDING_BORDER_OPACITY = c.comment("Percent.").defineInRange("buildingBorderOpacity", 95, 0, 100);
        HIDDEN_BUILDING_TYPES = c.comment("Building type ids to skip, e.g. minecolonies:postbox or just postbox.")
                .defineListAllowEmpty("hiddenBuildingTypes", List.of(), () -> "", o -> o instanceof String);
        BUILDING_ICONS = c.comment("Hut item icon at each footprint's centre.").define("buildingIcons", true);
        ICONS_ON_MINIMAP = c.define("iconsOnMinimap", true);
        ICONS_ON_WORLD_MAP = c.define("iconsOnWorldMap", true);
        LABEL_MODE_MINIMAP = c.comment("ALWAYS draws the label under the icon; HOVER uses the info display line.")
                .defineEnum("labelModeMinimap", LabelMode.ALWAYS);
        LABEL_MODE_WORLD_MAP = c.comment("ALWAYS draws the label under the icon; HOVER only when the mouse is over it.")
                .defineEnum("labelModeWorldMap", LabelMode.ALWAYS);
        WORLD_MAP_LABEL_MIN_ZOOM = c.comment("Hide world map labels (not icons) below this zoom (map pixels per block).")
                .defineInRange("worldMapLabelMinZoom", 1.0, 0.0, 64.0);
        ICON_SCALE = c.comment("Size of hut icons (and their labels).").defineInRange("iconScale", 1.0, 0.25, 4.0);
        ICON_BACKGROUND = c.comment("Black rounded badge with a white outline behind hut icons.")
                .define("iconBackground", true);
        LABEL_SCALE = c.comment("Label text size, relative to the icon.").defineInRange("labelScale", 1.0, 0.25, 4.0);
        c.pop();
        CLIENT_SPEC = c.build();
    }

    private Config() {
    }
}
