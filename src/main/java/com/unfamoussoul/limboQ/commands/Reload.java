package com.unfamoussoul.limboQ.commands;

import com.unfamoussoul.limboQ.LimboQ;
import com.unfamoussoul.limboQ.entities.Settings;

public class Reload extends AbstractCommand {
    private final LimboQ plugin = getPlugin();

    public Reload(LimboQ plugin) {
        super(plugin, "reload");
    }

    public void run(Invocation invocation) {
        try {
            plugin.reload();
            invocation.source().sendRichMessage(Settings.IMP.MESSAGES.RELOAD);
        } catch (Exception e) {invocation.source().sendRichMessage(Settings.IMP.MESSAGES.RELOAD_FAILED + e);}
    }
}
