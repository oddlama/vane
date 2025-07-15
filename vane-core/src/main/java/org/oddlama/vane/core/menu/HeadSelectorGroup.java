package org.oddlama.vane.core.menu;

import org.bukkit.Material;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.config.TranslatedItemStack;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.ModuleComponent;

public class HeadSelectorGroup extends ModuleComponent<Core> {

    @LangMessage
    public TranslatedMessage lang_title;

    @LangMessage
    public TranslatedMessage lang_filter_title;

    public TranslatedItemStack<?> item_select_head;

    public HeadSelectorGroup(Context<Core> context) {
        super(context.namespace("head_selector", "Menu configuration for the head selector menu."));
        item_select_head = new TranslatedItemStack<>(
            get_context(),
            "select_head",
            Material.BARRIER,
            1,
            "Used to represent a head in the head library."
        );
    }

    @Override
    public void on_enable() {}

    @Override
    public void on_disable() {}
}
