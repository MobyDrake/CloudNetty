package ru.mobydrake.client.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import ru.mobydrake.client.util.FileObj;
import ru.mobydrake.common.AuthRequest;
import ru.mobydrake.common.FileMessage;
import ru.mobydrake.common.ListRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ObservableList<FileObj> listServerFiles;
    private ObservableList<FileObj> listLocalFiles;
    private BooleanProperty auth;
    private BooleanProperty connected;
    private static final String STORAGE = "client/storage/";

    public ClientHandler(BooleanProperty connected, BooleanProperty auth, ObservableList<FileObj> listServerFiles, ObservableList<FileObj> listLocalFiles) {
        this.connected = connected;
        this.auth = auth;
        this.listServerFiles = listServerFiles;
        this.listLocalFiles = listLocalFiles;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;

        if (msg instanceof ListRequest) {
            listMessage((ListRequest) msg);
        }
        if (msg instanceof FileMessage) {
            fileMessage((FileMessage) msg);
        }
        if (msg instanceof AuthRequest) {
            autRequest((AuthRequest) msg);
        }
    }

    private void autRequest(AuthRequest msg) {
        auth.set(msg.isAuth());
    }

    private void fileMessage(FileMessage msg) throws IOException {
        Path path = Paths.get(STORAGE + msg.getFileName());
        Files.write(path, msg.getData(), StandardOpenOption.CREATE);
        Platform.runLater(() -> listLocalFiles.add(new FileObj(path)));
    }

    private void listMessage(ListRequest msg) {
        Platform.runLater(() -> {
            listServerFiles.clear();
            //msg.getList().stream().map(FileObj::new).forEach(listServerFiles::add);
            for(String str : msg.getList()) {
                listServerFiles.add(new FileObj(str));
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        auth.set(false);
        connected.set(false);
    }
}
