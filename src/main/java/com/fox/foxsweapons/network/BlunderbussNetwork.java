package com.fox.foxsweapons.network;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.BlunderbussItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BlunderbussNetwork {

    private BlunderbussNetwork() {
    }

    /*
     * =========================================================
     * REGISTER
     * =========================================================
     */

    public static void registerPayloads(
            RegisterPayloadHandlersEvent event) {

        PayloadRegistrar registrar =
                event.registrar("1");

        /*
         * LEFT CLICK
         */
        registrar.playToServer(
                SingleFirePayload.TYPE,
                SingleFirePayload.STREAM_CODEC,

                (payload, context) ->
                        handleFire(
                                context,
                                false
                        )
        );

        /*
         * RIGHT CLICK
         */
        registrar.playToServer(
                BurstFirePayload.TYPE,
                BurstFirePayload.STREAM_CODEC,

                (payload, context) ->
                        handleFire(
                                context,
                                true
                        )
        );
    }

    /*
     * =========================================================
     * SERVER HANDLER
     * =========================================================
     */

    private static void handleFire(
            IPayloadContext context,
            boolean burst) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }

        BlunderbussItem.fire(
                player,
                burst
        );
    }

    /*
     * =========================================================
     * LEFT CLICK PACKET
     * =========================================================
     *
     * There is no data inside this packet.
     *
     * Its existence simply means:
     *
     * "fire one pellet"
     */

    public record SingleFirePayload()
            implements CustomPacketPayload {

        public static final Type<SingleFirePayload>
                TYPE =

                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                FoxsWeapons.MODID,
                                "blunderbuss_single_fire"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                SingleFirePayload> STREAM_CODEC =

                StreamCodec.unit(
                        new SingleFirePayload()
                );

        @Override
        public Type<? extends CustomPacketPayload>
        type() {

            return TYPE;
        }
    }

    /*
     * =========================================================
     * RIGHT CLICK PACKET
     * =========================================================
     *
     * Means:
     *
     * "fire four pellets"
     */

    public record BurstFirePayload()
            implements CustomPacketPayload {

        public static final Type<BurstFirePayload>
                TYPE =

                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                FoxsWeapons.MODID,
                                "blunderbuss_burst_fire"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                BurstFirePayload> STREAM_CODEC =

                StreamCodec.unit(
                        new BurstFirePayload()
                );

        @Override
        public Type<? extends CustomPacketPayload>
        type() {

            return TYPE;
        }
    }
}