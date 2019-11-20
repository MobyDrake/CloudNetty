package ru.mobydrake.client.handlers;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import ru.mobydrake.common.utils.FileObj;
import ru.mobydrake.common.messages.AuthRequest;
import ru.mobydrake.common.messages.FileMessage;
import ru.mobydrake.common.messages.ListRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import static ru.mobydrake.client.ClientController.updateUI;


public class ClientHandler extends ChannelInboundHandlerAdapter {
    private ObservableList<FileObj> listServerFiles;
    private ObservableList<FileObj> listLocalFiles;
    private BooleanProperty auth;
    private BooleanProperty connected;
    private StringProperty status;
    private StringProperty STORAGE;

    public ClientHandler(BooleanProperty connected, BooleanProperty auth, ObservableList<FileObj> listServerFiles,
                         ObservableList<FileObj> listLocalFiles, StringProperty status, StringProperty STORAGE) {
        this.connected = connected;
        this.auth = auth;
        this.listServerFiles = listServerFiles;
        this.listLocalFiles = listLocalFiles;
        this.status = status;
        this.STORAGE = STORAGE;
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

    private void autRequest(AuthRequest msg) throws IOException {
        if (!msg.isAuth()) {
            updateUI(() -> status.setValue("Wrong login or password"));
        }
        else {
            Path path = Paths.get("client/storage/" + msg.getLogin());
            if (Files.notExists(path)) {
                Files.createDirectory(path);
            }
            STORAGE.setValue(path + "/");
            refreshLocalFiles();
        }
        auth.set(msg.isAuth());
    }

    private void fileMessage(FileMessage msg) throws IOException {
        Path path = Paths.get(STORAGE.getValue() + msg.getFileName());
        Files.write(path, msg.getData(), StandardOpenOption.CREATE);
        refreshLocalFiles();
    }

    private void listMessage(ListRequest msg) {
        updateUI(() -> {
            listServerFiles.clear();
            //msg.getList().stream().map(FileObj::new).forEach(listServerFiles::add);
            for(String str : msg.getList()) {
                listServerFiles.add(new FileObj(str));
            }
        });
    }

    private void refreshLocalFiles() {
        updateUI(() -> {
            try {
                listLocalFiles.clear();
                Files.list(Paths.get(STORAGE.getValue())).map(FileObj::new).forEach(o -> listLocalFiles.add(o));
            } catch (IOException e) {
                e.printStackTrace();
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
