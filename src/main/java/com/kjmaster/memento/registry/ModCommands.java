package com.kjmaster.memento.registry;

import com.kjmaster.memento.Memento;
import com.kjmaster.memento.api.MementoAPI;
import com.kjmaster.memento.component.TrackerMap;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

@EventBusSubscriber(modid = Memento.MODID)
public class ModCommands {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        dispatcher.register(Commands.literal("memento")
                .requires(source -> source.hasPermission(2)) // OP Level 2

                // Subcommand: QUERY (Check stat on held item)
                .then(Commands.literal("query")
                        .then(Commands.argument("stat", ResourceLocationArgument.id())
                                .executes(ModCommands::queryStat)))

                // Subcommand: ADD (Increment stat on held item)
                .then(Commands.literal("add")
                        .then(Commands.argument("stat", ResourceLocationArgument.id())
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ModCommands::addStat))))

                // Subcommand: SET (Set stat on held item)
                .then(Commands.literal("set")
                        .then(Commands.argument("stat", ResourceLocationArgument.id())
                                .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                        .executes(ModCommands::setStat))))
        );
    }

    private static int queryStat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation statId = ResourceLocationArgument.getId(context, "stat");
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item."));
            return 0;
        }

        TrackerMap trackers = stack.getOrDefault(ModDataComponents.TRACKER_MAP, TrackerMap.EMPTY);
        long value = trackers.getValue(statId);

        context.getSource().sendSuccess(() -> Component.literal("Stat " + statId + ": " + value), false);
        return (int) value;
    }

    private static int addStat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation statId = ResourceLocationArgument.getId(context, "stat");
        long amount = LongArgumentType.getLong(context, "amount");
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item."));
            return 0;
        }

        // Use the API to ensure Milestones trigger!
        MementoAPI.incrementStat(player, stack, statId, amount);

        context.getSource().sendSuccess(() -> Component.literal("Added " + amount + " to " + statId), true);
        return 1;
    }

    private static int setStat(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        ServerPlayer player = context.getSource().getPlayerOrException();
        ResourceLocation statId = ResourceLocationArgument.getId(context, "stat");
        long target = LongArgumentType.getLong(context, "amount");
        ItemStack stack = player.getMainHandItem();

        if (stack.isEmpty()) {
            context.getSource().sendFailure(Component.literal("You must hold an item."));
            return 0;
        }

        // For SET, we need to calculate the difference if we want to trigger milestones correctly via increment,
        // OR we just set it directly.
        // Using updateStat with a generic replacement function is safest.
        MementoAPI.updateStat(player, stack, statId, target, (oldVal, newVal) -> newVal);

        context.getSource().sendSuccess(() -> Component.literal("Set " + statId + " to " + target), true);
        return 1;
    }
}