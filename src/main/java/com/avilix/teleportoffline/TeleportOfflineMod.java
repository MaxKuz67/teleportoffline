package com.avilix.teleportoffline;

import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;

import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import static net.minecraft.commands.Commands.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(TeleportOfflineMod.MODID)
public class TeleportOfflineMod {
    public static final String MODID = "teleportoffline";
    private static final Logger LOGGER = LogManager.getLogger(MODID);

    public TeleportOfflineMod() {
        NeoForge.EVENT_BUS.register(this);

        LOGGER.info("[TeleportOffline] TeleportOffline loaded successfully");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("TeleportOffline start");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                literal("tpoffline")
                        .requires(src ->
                                src.hasPermission(2) ||
                                        PermissionManager.hasPerm(src, "teleportoffline.tp")
                        )
                        .then(argument("playerName", StringArgumentType.word())
                                .then(argument("x", DoubleArgumentType.doubleArg())
                                        .then(argument("y", DoubleArgumentType.doubleArg())
                                                .then(argument("z", DoubleArgumentType.doubleArg())
                                                        .executes(ctx -> {
                                                            // дублируем проверку на всякий случай
                                                            if (!ctx.getSource().hasPermission(2)
                                                                    && !PermissionManager.hasPerm(ctx.getSource(), "teleportoffline.tp")) {
                                                                ctx.getSource()
                                                                        .sendFailure(Component.literal(
                                                                                "У вас нет permission teleportoffline.tp"
                                                                        ));
                                                                return 0;
                                                            }

                                                            String name = StringArgumentType.getString(ctx, "playerName");
                                                            double x = DoubleArgumentType.getDouble(ctx, "x");
                                                            double y = DoubleArgumentType.getDouble(ctx, "y");
                                                            double z = DoubleArgumentType.getDouble(ctx, "z");
                                                            return TeleportOfflineHandler.teleport(
                                                                    ctx.getSource(), name, x, y, z
                                                            );
                                                        })
                                                )
                                        )
                                )
                        )
        );
    }

}
