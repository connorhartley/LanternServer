package org.lanternpowered.server.network.pipeline;

import java.util.List;

import org.lanternpowered.server.network.message.Message;
import org.lanternpowered.server.network.message.MessageRegistration;
import org.lanternpowered.server.network.message.caching.CachingHashGenerator;
import org.lanternpowered.server.network.message.codec.Codec;
import org.lanternpowered.server.network.message.codec.CodecContext;
import org.lanternpowered.server.network.message.processor.Processor;
import org.lanternpowered.server.network.protocol.Protocol;
import org.lanternpowered.server.network.session.Session;

import com.google.common.base.Optional;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.MessageToMessageCodec;
import io.netty.util.AttributeKey;

@SuppressWarnings({ "rawtypes", "unchecked" })
public final class MessageCodecHandler extends MessageToMessageCodec<ByteBuf, Message> {

    public static final AttributeKey<CodecContext> CONTEXT = AttributeKey.valueOf("codec-context");

    @Override
    protected void encode(ChannelHandlerContext ctx, Message message, List<Object> output) throws Exception {
        Protocol protocol = ctx.channel().attr(Session.STATE).get().getProtocol();
        MessageRegistration registration = protocol.outbound().find(message.getClass());

        if (registration.getOpcode() == null) {
            throw new EncoderException("Message type (" + message.getClass().getName() + ") is not registered to allow encoding!");
        }

        ByteBuf opcode = ctx.alloc().buffer();

        // Write the opcode of the message
        CodecContext context = ctx.channel().attr(CONTEXT).get();
        context.writeVarInt(opcode, registration.getOpcode());

        Codec codec = registration.getCodec();
        ByteBuf content = null;

        // Handle first the caching system
        Optional<CachingHashGenerator<?>> hashGen = CachedMessages.getHashGenerator(codec.getClass());
        if (hashGen.isPresent()) {
            int hash = ((CachingHashGenerator) hashGen.get()).generate(context, message);
            content = CachedMessages.getCachedMessage(message).getEncodedMessage(hash);
        }
        if (content == null) {
            // Write the content of the message
            content = registration.getCodec().encode(context, message);
        }

        // Add the buffer to the output
        output.add(Unpooled.wrappedBuffer(opcode, content));
    }

    @Override
    public void decode(ChannelHandlerContext ctx, ByteBuf input, List<Object> output) throws Exception {
        if (input.readableBytes() == 0) {
            return;
        }

        CodecContext context = ctx.channel().attr(CONTEXT).get();
        int opcode = context.readVarInt(input);

        Protocol protocol = ctx.channel().attr(Session.STATE).get().getProtocol();
        MessageRegistration registration = protocol.inbound().find(opcode);

        if (registration == null) {
            throw new DecoderException("Failed to find a message registration with opcode " + opcode + "!");
        }

        // Copy the remaining content of the buffer to a new buffer used by the
        // message decoding
        ByteBuf content = ctx.alloc().buffer(input.readableBytes());
        input.readBytes(content, input.readableBytes());

        // Read the content of the message
        Message message = registration.getCodec().decode(context, content);

        Processor processor = registration.getProcessor();
        // Only process if there are processors found
        if (processor != null) {
            // The processor should handle the output messages
            processor.process(context, message, output);
        } else {
            // Add the message to the output
            output.add(message);
        }
    }
}