package com.example.scarecrowbot.util;

import com.example.scarecrowbot.ScarecrowBotPlugin;
import lombok.experimental.UtilityClass;
import org.bukkit.NamespacedKey;

/**
 * PersistentDataContainer key constants
 */
@UtilityClass
public class PDCKeys {

    /**
     * Marker to identify scarecrow entities
     */
    public static final NamespacedKey SCARECROW_MARKER = new NamespacedKey(ScarecrowBotPlugin.getInstance(), "scarecrow_marker");

    /**
     * Current HP of the scarecrow
     */
    public static final NamespacedKey SCARECROW_HP = new NamespacedKey(ScarecrowBotPlugin.getInstance(), "scarecrow_hp");

    /**
     * Maximum HP of the scarecrow
     */
    public static final NamespacedKey SCARECROW_MAX_HP = new NamespacedKey(ScarecrowBotPlugin.getInstance(), "scarecrow_max_hp");

    /**
     * Display name of the scarecrow
     */
    public static final NamespacedKey SCARECROW_NAME = new NamespacedKey(ScarecrowBotPlugin.getInstance(), "scarecrow_name");
}
