package ru.mobydrake.server.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import ru.mobydrake.common.FileMessage;
import ru.mobydrake.common.FileRequest;
import ru.mobydrake.common.ListMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    private final String STORAGE = "server/server_storage/";
    private List<String> list = new ArrayList<>();

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }

        if (msg instanceof FileRequest) {
            fileRequest(ctx, (FileRequest) msg);
        }

        if (msg instanceof ListMessage) {
            listMessage(ctx, (ListMessage) msg);
        }
    }

    private void fileRequest(ChannelHandlerContext ctx, FileRequest msg) throws IOException {
        Path pathFile = Paths.get(STORAGE + msg.getName());
        if (Files.exists(pathFile)) {
            FileMessage fm = new FileMessage(pathFile);
            ctx.writeAndFlush(fm);
        } else {
            System.out.println("error");
        }
    }

    private void listMessage(ChannelHandlerContext ctx, ListMessage msg) throws IOException {
        list.clear();
        Files.list(Paths.get(STORAGE)).map(p -> p.getFileName().toString()).forEach(list::add);
        msg.setList(list);
        ctx.writeAndFlush(msg);

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
