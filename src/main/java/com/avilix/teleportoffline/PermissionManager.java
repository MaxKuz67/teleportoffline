package com.avilix.teleportoffline;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

import java.util.concurrent.ExecutionException;

public class PermissionManager {

    /**
     * Проверяет, есть ли у источника команды (игрока) в LuckPerms разрешение node.
     * Консоль всегда получает true.
     */
    public static boolean hasPerm(CommandSourceStack src, String node) {
        if (src.getEntity() instanceof ServerPlayer player) {
            LuckPerms lp = LuckPermsProvider.get();

            // 1) Получаем (или загружаем) User из LuckPerms по UUID
            User user = lp.getUserManager().getUser(player.getUUID());
            if (user == null) {
                try {
                    user = lp.getUserManager()
                            .loadUser(player.getUUID())
                            .get();
                } catch (InterruptedException | ExecutionException e) {
                    return false;
                }
            }

            QueryOptions opts = lp.getContextManager().getQueryOptions(user)
                    .orElseGet(lp.getContextManager()::getStaticQueryOptions);

            return user.getCachedData()
                    .getPermissionData(opts)
                    .checkPermission(node)
                    .asBoolean();
        }


        return true;
    }
}