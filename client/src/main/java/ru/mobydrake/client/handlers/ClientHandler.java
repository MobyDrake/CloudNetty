package ru.mobydrake.client.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import ru.mobydrake.client.util.FileObj;
import ru.mobydrake.common.FileMessage;
import ru.mobydrake.common.ListMessage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;


public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ObservableList<FileObj> listServerFiles;
    private ObservableList<FileObj> listLocalFiles;
    private final String STORAGE = "client/storage/";

    public ClientHandler(ObservableList<FileObj> listServerFiles, ObservableList<FileObj> listLocalFiles) {
        this.listServerFiles = listServerFiles;
        this.listLocalFiles = listLocalFiles;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg == null) return;

        if (msg instanceof ListMessage) {
            listMessage((ListMessage) msg);
        }
        if (msg instanceof FileMessage) {
            fileMessage((FileMessage) msg);
        }
    }

    private void fileMessage(FileMessage msg) throws IOException {
        Path path = Paths.get(STORAGE + msg.getFileName());
        Files.write(path, msg.getData(), StandardOpenOption.CREATE);
        Platform.runLater(() -> listLocalFiles.add(new FileObj(path)));
    }

    private void listMessage(ListMessage msg) {
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
}
