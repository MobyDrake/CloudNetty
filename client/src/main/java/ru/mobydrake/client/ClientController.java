package ru.mobydrake.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import ru.mobydrake.client.handlers.ClientHandler;
import ru.mobydrake.client.util.FileObj;
import ru.mobydrake.common.FileRequest;
import ru.mobydrake.common.ListMessage;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Paths;


public class ClientController {

    @FXML
    private TableView<FileObj> fileLocalTable;
    @FXML
    private TableColumn<FileObj, String> fileNameLocalColumn;
    @FXML
    private TableColumn<FileObj, Long> fileSizeLocalColumn;
    @FXML
    private TableView<FileObj> fileServerTable;
    @FXML
    private TableColumn<FileObj, String> fileNameServerColumn;
    //TODO: отображение размера файла на сервере
    @FXML
    private TableColumn<FileObj, Long> fileSizeServerColumn;

    private ObservableList<FileObj> listLocalFiles = FXCollections.observableArrayList();
    private ObservableList<FileObj> listServerFiles = FXCollections.observableArrayList();

    private final String ADDRESS = "localhost";
    private final int PORT = 8182;
    private final String STORAGE = "client/storage";
    private EventLoopGroup workerGroup;
    private Channel channel;


    @FXML
    private void initialize() {
        fileServerTable.setItems(listServerFiles);
        fileNameServerColumn.setCellValueFactory(cellData -> cellData.getValue().getFileName());

        fileLocalTable.setItems(listLocalFiles);
        fileNameLocalColumn.setCellValueFactory(cellData -> cellData.getValue().getFileName());
        fileSizeLocalColumn.setCellValueFactory(cellData -> cellData.getValue().getSize().asObject());
        refreshLocalFiles();
        connect();
    }

    private void connect() {
        workerGroup = new NioEventLoopGroup();

        Task<Channel> task = new Task<Channel>() {
            @Override
            protected Channel call() throws Exception {
                Bootstrap bootstrap = new Bootstrap();
                bootstrap.group(workerGroup);
                bootstrap.channel(NioSocketChannel.class);
                bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
                //освобождает от указания адреса и порта в bootstrap.connect(HOST, PORT)
                bootstrap.remoteAddress(new InetSocketAddress(ADDRESS, PORT));

                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                new ObjectEncoder(),
                                new ClientHandler(listServerFiles, listLocalFiles));
                    }
                });

                ChannelFuture future = bootstrap.connect();
                Channel chn = future.channel();

                future.sync();

                return chn;
            }

            @Override
            protected void succeeded() {
                channel = getValue();
                refreshServerFiles();
            }

            @Override
            protected void failed() {
                Throwable exc = getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText( exc.getClass().getName() );
                alert.setContentText( exc.getMessage() );
                alert.showAndWait();

            }
        };
        new Thread(task).start();
    }

    @FXML
    private void refreshLocalFiles() {
        updateUI(() -> {
                try {
                    fileLocalTable.getItems().clear();
                    Files.list(Paths.get(STORAGE)).map(FileObj::new).forEach(o -> listLocalFiles.add(o));
                } catch (IOException e) {
                    e.printStackTrace();
                }
        });
    }

    @FXML
    private void refreshServerFiles() {
        channel.writeAndFlush(new ListMessage());
    }

    @FXML
    private void downloadFile() {
        channel.writeAndFlush(new FileRequest(fileServerTable.getSelectionModel().getSelectedItem().getFileName().getValue()));
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

}