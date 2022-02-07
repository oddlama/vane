package org.oddlama.vane.core;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class PacketHttpInterceptor extends ByteToMessageDecoder {
	private boolean identifiedHttpStream = false;
	private boolean interceptHttpMessage(ChannelHandlerContext ctx, byte[] data) {
		if (data.length < 3 || data[0] != (byte)'G' || data[1] != (byte)'E' || data[2] != (byte)'T' || data[3] != (byte)' ' || data[4] != (byte)'/') {
			return false;
		}

		System.out.println("identified and intercepted http stream from " + ctx.channel() + "!");
		var request = new String(data, StandardCharsets.UTF_8);

        final ByteBuf buf = ctx.alloc().buffer(4);
        buf.writeBytes("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=utf-8\r\nContent-Length: 72\r\n\r\n".getBytes());
        buf.writeBytes("<html>🚀🌌 Is this real life or is this just fantasyyyyy?</html>".getBytes());
		ctx.writeAndFlush(buf).addListener(ChannelFutureListener.CLOSE);
		return true;
	}
	protected void decode(ChannelHandlerContext ctx, ByteBuf buf, List<Object> out) throws Exception {
		if (buf.readableBytes() != 0) {
			var data = new byte[buf.readableBytes()];
			buf.readBytes(data);
			if (identifiedHttpStream) {
				var request = new String(data, StandardCharsets.UTF_8);
				System.out.println("discarding extraneous http data: " + request);
			}
			if (identifiedHttpStream || interceptHttpMessage(ctx, data)) {
				identifiedHttpStream = true;
				return;
			}
			out.add(Unpooled.wrappedBuffer(data));
		}
	}
}
