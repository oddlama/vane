package org.oddlama.vane.regions.menu;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.annotation.config.ConfigMaterial;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.menu.Menu;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.menu.MenuFactory;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;
import org.oddlama.vane.regions.Regions;

public class EnterRoleNameMenu extends ModuleComponent<Regions> {

    @LangMessage
    public TranslatedMessage lang_title;

    @ConfigMaterial(def = Material.BOOK, desc = "The item used to name roles.")
    public Material config_material;

    public EnterRoleNameMenu(Context<Regions> context) {
        super(context.namespace("enter_role_name"));
    }

    public Menu create(final Player player, final Function2<Player, String, ClickResult> on_click) {
        return create(player, "Role", on_click);
    }

    public Menu create(
        final Player player,
        final String default_name,
        final Function2<Player, String, ClickResult> on_click
    ) {
        return MenuFactory.anvil_string_input(
            get_context(),
            player,
            lang_title.str(),
            new ItemStack(config_material),
            default_name,
            (p, menu, name) -> {
                menu.close(p);
                return on_click.apply(p, name);
            }
        );
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
