package com.unfamoussoul.limboQ.commands;

import com.unfamoussoul.limboQ.LimboQ;

public class Reload extends AbstractCommand {
    private final LimboQ plugin;

    public Reload(LimboQ plugin) {
        super("reload");
        this.plugin = plugin;
    }

    public void run(Invocation invocation) {
        try {
            plugin.reload();
            invocation.source().sendRichMessage(plugin.config.LOCALE_RELOAD);
        } catch (Exception e) {invocation.source().sendRichMessage(plugin.config.LOCALE_RELOAD_FAILED + e);}
    }
}
