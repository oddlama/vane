package org.oddlama.vane.core.menu;

import java.util.List;
import java.util.stream.Collectors;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.oddlama.vane.core.functional.Function2;
import org.oddlama.vane.core.menu.Menu.ClickResult;
import org.oddlama.vane.core.module.Context;

public interface Filter<T> {
    public void open_filter_settings(
        final Context<?> context,
        final Player player,
        final String filter_title,
        final Menu return_to
    );

    public void reset();

    public List<T> filter(final List<T> things);

    public static class StringFilter<T> implements Filter<T> {

        private String str = null;
        private Function2<T, String, Boolean> do_filter;
        private boolean ignore_case;

        public StringFilter(final Function2<T, String, Boolean> do_filter) {
            this(do_filter, true);
        }

        public StringFilter(final Function2<T, String, Boolean> do_filter, boolean ignore_case) {
            this.do_filter = do_filter;
            this.ignore_case = ignore_case;
        }

        @Override
        public void open_filter_settings(
            final Context<?> context,
            final Player player,
            final String filter_title,
            final Menu return_to
        ) {
            MenuFactory.anvil_string_input(
                context,
                player,
                filter_title,
                new ItemStack(Material.PAPER),
                "?",
                (p, menu, s) -> {
                    menu.close(p);
                    str = s;
                    return_to.open(p);
                    return ClickResult.SUCCESS;
                }
            ).open(player);
        }

        @Override
        public void reset() {
            str = null;
        }

        @Override
        public List<T> filter(final List<T> things) {
            if (str == null) {
                return things;
            } else {
                final String f_str;
                if (ignore_case) {
                    f_str = str.toLowerCase();
                } else {
                    f_str = str;
                }

                return things.stream().filter(t -> do_filter.apply(t, f_str)).collect(Collectors.toList());
            }
        }
    }
}
