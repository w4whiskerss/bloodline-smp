package dev.zahen.bloodline.ability;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;

public interface Bloodline {

    void applyPassive(Player player);

    void removePassive(Player player);

    void handlePrimaryAbility(Player player);

    void handleSecondaryAbility(Player player);

    default boolean supportsSpecialAbility() {
        return false;
    }

    default void handleSpecialAbility(Player player) {
    }

    default boolean supportsFourthAbility() {
        return false;
    }

    default void handleFourthAbility(Player player) {
    }

    default boolean supportsFifthAbility() {
        return false;
    }

    default void handleFifthAbility(Player player) {
    }

    List<Component> describePassives(Player player);
}
