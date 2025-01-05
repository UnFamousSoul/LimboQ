package com.unfamoussoul.limboQ.commands;

import com.unfamoussoul.limboQ.LimboQ;

public class Reload extends AbstractCommand {
    private final LimboQ plugin = getPlugin();

    public Reload(LimboQ plugin) {
        super(plugin, "reload");
    }

    public void run(Invocation invocation) {
        try {
            plugin.reload();
            invocation.source().sendRichMessage(plugin.settings.LOCALE_RELOAD);
        } catch (Exception e) {invocation.source().sendRichMessage(plugin.settings.LOCALE_RELOAD_FAILED + e);}
    }
}
