package com.fox.foxsweapons.network;

import com.fox.foxsweapons.FoxsWeapons;
import com.fox.foxsweapons.item.BoneShivItem;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.minecraft.resources.Identifier;

import net.minecraft.server.level.ServerPlayer;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public final class BoneShivNetwork {

    private BoneShivNetwork() {
    }

    // =========================================================
    // REGISTER
    // =========================================================

    public static void registerPayloads(
            PayloadRegistrar registrar
    ) {

        registrar.playToServer(
                AttackPayload.TYPE,
                AttackPayload.STREAM_CODEC,

                BoneShivNetwork::handleAttack
        );
    }

    // =========================================================
    // SERVER HANDLER
    // =========================================================

    private static void handleAttack(
            AttackPayload payload,
            IPayloadContext context
    ) {

        if (!(context.player()
                instanceof ServerPlayer player)) {

            return;
        }

        BoneShivItem.attack(
                player,
                payload.targetEntityId(),
                payload.physicalLeft()
        );
    }

    // =========================================================
    // ATTACK PACKET
    // =========================================================
    //
    // targetEntityId:
    // which entity the client clicked
    //
    // physicalLeft:
    //
    // true  = LEFT mouse button
    //         physical LEFT Shiv
    //
    // false = RIGHT mouse button
    //         physical RIGHT Shiv
    // =========================================================

    public record AttackPayload(
            int targetEntityId,
            boolean physicalLeft
    ) implements CustomPacketPayload {

        public static final Type<AttackPayload>
                TYPE =

                new Type<>(
                        Identifier.fromNamespaceAndPath(
                                FoxsWeapons.MODID,
                                "bone_shiv_attack"
                        )
                );

        public static final StreamCodec<
                RegistryFriendlyByteBuf,
                AttackPayload
                > STREAM_CODEC =

                new StreamCodec<>() {

                    @Override
                    public AttackPayload decode(
                            RegistryFriendlyByteBuf buffer
                    ) {

                        return new AttackPayload(
                                buffer.readVarInt(),
                                buffer.readBoolean()
                        );
                    }

                    @Override
                    public void encode(
                            RegistryFriendlyByteBuf buffer,
                            AttackPayload payload
                    ) {

                        buffer.writeVarInt(
                                payload.targetEntityId()
                        );

                        buffer.writeBoolean(
                                payload.physicalLeft()
                        );
                    }
                };

        @Override
        public Type<? extends CustomPacketPayload>
        type() {

            return TYPE;
        }
    }
}