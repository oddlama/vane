package org.oddlama.vane.core.commands;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;
import static io.papermc.paper.command.brigadier.Commands.argument;
import static io.papermc.paper.command.brigadier.Commands.literal;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import java.util.Random;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.loot.LootContext;
import org.bukkit.loot.LootTables;
import org.oddlama.vane.annotation.command.Name;
import org.oddlama.vane.annotation.lang.LangMessage;
import org.oddlama.vane.core.Core;
import org.oddlama.vane.core.command.Command;
import org.oddlama.vane.core.command.argumentType.ModuleArgumentType;
import org.oddlama.vane.core.lang.TranslatedMessage;
import org.oddlama.vane.core.module.Context;
import org.oddlama.vane.core.module.Module;

@Name("vane")
public class Vane extends Command<Core> {

    @LangMessage
    private TranslatedMessage lang_reload_success;

    @LangMessage
    private TranslatedMessage lang_reload_fail;

    @LangMessage
    private TranslatedMessage lang_resource_pack_generate_success;

    @LangMessage
    private TranslatedMessage lang_resource_pack_generate_fail;

    public Vane(Context<Core> context) {
        super(context);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> get_command_base() {
        return super.get_command_base()
            .then(help())
            .then(
                literal("reload")
                    .executes(ctx -> {
                        reload_all(ctx.getSource().getSender());
                        return SINGLE_SUCCESS;
                    })
                    .then(
                        argument("module", ModuleArgumentType.module(get_module())).executes(ctx -> {
                            reload_module(ctx.getSource().getSender(), ctx.getArgument("module", Module.class));
                            return SINGLE_SUCCESS;
                        })
                    )
            )
            .then(
                literal("generate_resource_pack").executes(ctx -> {
                    generate_resource_pack(ctx.getSource().getSender());
                    return SINGLE_SUCCESS;
                })
            )
            .then(
                literal("test_do_not_use_if_you_are_not_a_dev").executes(ctx -> {
                    test(ctx.getSource().getSender());
                    return SINGLE_SUCCESS;
                })
            );
    }

    private void reload_module(CommandSender sender, Module<?> module) {
        if (module.reload_configuration()) {
            lang_reload_success.send(sender, "§bvane-" + module.get_name());
        } else {
            lang_reload_fail.send(sender, "§bvane-" + module.get_name());
        }
    }

    private void reload_all(CommandSender sender) {
        for (var m : get_module().core.get_modules()) {
            reload_module(sender, m);
        }
    }

    private void generate_resource_pack(CommandSender sender) {
        var file = get_module().generate_resource_pack();
        if (file != null) {
            lang_resource_pack_generate_success.send(sender, file.getAbsolutePath());
        } else {
            lang_resource_pack_generate_fail.send(sender);
        }
        if (sender instanceof Player) {
            var dist = get_module().resource_pack_distributor;
            dist.update_sha1(file);
            dist.send_resource_pack((Player) sender);
        }
    }

    private void test_tome_generation() {
        final var loot_table = LootTables.ABANDONED_MINESHAFT.getLootTable();
        final var inventory = get_module().getServer().createInventory(null, 3 * 9);
        final var context =
            (new LootContext.Builder(
                    get_module().getServer().getWorlds().get(0).getBlockAt(0, 0, 0).getLocation()
                )).build();
        final var random = new Random();

        int tomes = 0;
        final var simulation_count = 10000;
        final var gt_percentage = 0.2; // (0-2) (average 1) with 1/5 chance
        final var tolerance = 0.7;
        get_module().log.info("Testing ancient tome generation...");

        for (int i = 0; i < simulation_count; ++i) {
            inventory.clear();
            loot_table.fillInventory(inventory, random, context);
            for (final var is : inventory.getStorageContents()) {
                if (is != null && is.hasItemMeta()) {
                    final var meta = is.getItemMeta();
                    if (meta.hasCustomModelData() && is.getItemMeta().getCustomModelData() == 0x770000) {
                        ++tomes;
                    }
                }
            }
        }

        if (tomes == 0) {
            get_module().log.severe("0 tomes were generated in " + simulation_count + " chests.");
        } else if (
            tomes > gt_percentage * simulation_count * tolerance &&
            tomes < (gt_percentage * simulation_count) / tolerance
        ) { // 70% tolerance to lower bound
            get_module()
                .log.warning(
                    tomes +
                    " tomes were generated in " +
                    simulation_count +
                    " chests. This is " +
                    ((100.0 * ((double) tomes / simulation_count)) / gt_percentage) +
                    "% of the expected value."
                );
        } else {
            get_module()
                .log.info(
                    tomes +
                    " tomes were generated in " +
                    simulation_count +
                    " chests. This is " +
                    ((100.0 * ((double) tomes / simulation_count)) / gt_percentage) +
                    "% of the expected value."
                );
        }
    }

    private void test(CommandSender sender) {
        test_tome_generation();
    }
}
