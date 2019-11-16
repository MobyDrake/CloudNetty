package ru.mobydrake.server.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedInput;
import ru.mobydrake.common.*;
import ru.mobydrake.server.AuthService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ServerHandler extends ChannelInboundHandlerAdapter {
    //private final String STORAGE = "server/server_storage/";
    private static String STORAGE;
    private String user;
    private List<String> list = new ArrayList<>();


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) {
            return;
        }

        if (msg instanceof AuthRequest) {
            authRequest(ctx, (AuthRequest) msg);
        }

        if (msg instanceof FileRequest) {
            fileRequest(ctx, (FileRequest) msg);
        }

        if (msg instanceof ListRequest) {
            listMessage(ctx, (ListRequest) msg);
        }

        if (msg instanceof FileMessage) {
            fileMessage(ctx, (FileMessage) msg);
        }

        if (msg instanceof FileDelete) {
            deleteFile(ctx, (FileDelete) msg);
        }
//        if (msg instanceof ChunkedInput) {
//            File file = new File(STORAGE + "testChunk.mkv");
//            ByteBuf byteBuf = (ByteBuf) msg;
//            ByteBuffer byteBuffer = byteBuf.nioBuffer();
//            RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
//            FileChannel fileChannel = randomAccessFile.getChannel();
//
//            while (byteBuffer.hasRemaining()){;
//                fileChannel.position(file.length());
//                fileChannel.write(byteBuffer);
//            }
//
//            byteBuf.release();
//            fileChannel.close();
//            randomAccessFile.close();
//        }
    }


    private void authRequest(ChannelHandlerContext ctx, AuthRequest msg) throws IOException {
        if (AuthService.auth(msg.getLogin(), msg.getPassword())) {
            msg.setAuth(true);
            STORAGE = "server/server_storage/" + msg.getLogin() + "/";
            ctx.writeAndFlush(msg);
            listMessage(ctx, new ListRequest());
        } else {
            ctx.close();
        }
    }

    private void deleteFile(ChannelHandlerContext ctx, FileDelete msg) throws IOException {
        Path path = Paths.get(STORAGE + msg.getFileName());
        if (Files.exists(path)) {
            Files.delete(Paths.get(STORAGE + msg.getFileName()));
        }
        listMessage(ctx, new ListRequest());
    }

    private void fileMessage(ChannelHandlerContext ctx, FileMessage msg) throws IOException {
        if (Files.notExists(Paths.get(STORAGE + msg.getFileName()))) {
            Files.write(Paths.get(STORAGE + msg.getFileName()), msg.getData(), StandardOpenOption.CREATE);
            listMessage(ctx, new ListRequest());
        }
    }

    private void fileRequest(ChannelHandlerContext ctx, FileRequest msg) throws IOException {
        Path pathFile = Paths.get(STORAGE + msg.getName());
        if (Files.exists(pathFile)) {
            FileMessage fm = new FileMessage(pathFile);
            ctx.writeAndFlush(fm);
        } else {
            System.out.println("File is not exists");
        }
    }

    private void listMessage(ChannelHandlerContext ctx, ListRequest msg) throws IOException {
        if (Files.notExists(Paths.get(STORAGE))) {
            Files.createDirectory(Paths.get(STORAGE));
        }
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
