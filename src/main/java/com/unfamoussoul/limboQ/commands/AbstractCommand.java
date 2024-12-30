package com.unfamoussoul.limboQ.commands;

import com.google.common.collect.ImmutableList;
import com.unfamoussoul.limboQ.LimboQ;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;

import java.util.List;

public abstract class AbstractCommand implements SimpleCommand {
    private final LimboQ plugin;
    private final String slug;

    public AbstractCommand(LimboQ plugin, String slug) {
        this.plugin = plugin;
        this.slug = slug;
    }

    public LimboQ getPlugin() {
        return plugin;
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        if (invocation.arguments().length == 0) {
            return ImmutableList.of(slug);
        }

        return ImmutableList.of();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0 || !source.hasPermission("limboq." + slug)) return;

        run(invocation);
    }

    public abstract void run(Invocation invocation);
}
