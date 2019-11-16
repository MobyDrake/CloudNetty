package ru.mobydrake.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.stream.ChunkedFile;
import io.netty.handler.stream.ChunkedWriteHandler;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.WindowEvent;
import ru.mobydrake.client.handlers.ClientHandler;
import ru.mobydrake.client.util.FileObj;
import ru.mobydrake.common.*;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;


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
    @FXML
    private VBox boxAuth;
    @FXML
    private VBox boxMain;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;

    private BooleanProperty connected = new SimpleBooleanProperty(false);
    private BooleanProperty authentication = new SimpleBooleanProperty(false);
    private ObservableList<FileObj> listLocalFiles = FXCollections.observableArrayList();
    private ObservableList<FileObj> listServerFiles = FXCollections.observableArrayList();

    private final String ADDRESS = "localhost";
    private final int PORT = 8182;
    private final String STORAGE = "client/storage/";
    private EventLoopGroup workerGroup;
    private Channel channel;


    @FXML
    private void initialize() {
        boxAuth.visibleProperty().bind(connected);
        boxAuth.managedProperty().bind(connected);
        boxAuth.visibleProperty().bind(authentication.not());
        boxAuth.managedProperty().bind(authentication.not());

        boxMain.visibleProperty().bind(authentication);
        boxMain.managedProperty().bind(authentication);

        fileServerTable.setItems(listServerFiles);
        fileNameServerColumn.setCellValueFactory(cellData -> cellData.getValue().getFileName());

        fileLocalTable.setItems(listLocalFiles);
        fileNameLocalColumn.setCellValueFactory(cellData -> cellData.getValue().getFileName());
        fileSizeLocalColumn.setCellValueFactory(cellData -> cellData.getValue().getSize().asObject());
        refreshLocalFiles();
    }

    EventHandler<WindowEvent> getCloseEvent() {
        return new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                disconnect();
            }
        };
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
                bootstrap.remoteAddress(new InetSocketAddress(ADDRESS, PORT));

                bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel socketChannel) throws Exception {
                        socketChannel.pipeline().addLast(
                                new ObjectDecoder(50 * 1024 * 1024, ClassResolvers.cacheDisabled(null)),
                                new ChunkedWriteHandler(),
                                new ObjectEncoder(),
                                new ClientHandler(connected, authentication, listServerFiles, listLocalFiles));
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
                connected.set(true);

                channel.writeAndFlush(new AuthRequest(loginField.getText().trim(), passwordField.getText()));
                loginField.clear();
                passwordField.clear();
            }

            @Override
            protected void failed() {
                Throwable exc = getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Ошибка");
                alert.setHeaderText( exc.getClass().getName() );
                alert.setContentText( exc.getMessage() );
                alert.showAndWait();
                connected.set(false);
            }
        };
        new Thread(task).start();
    }

    private void disconnect() {
        if (!connected.get()) {
            workerGroup.shutdownGracefully();
            return;
        }

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                channel.close().sync();
                workerGroup.shutdownGracefully().sync();
                authentication.set(false);
                connected.set(false);
                return null;
            }

            @Override
            protected void failed() {
                Throwable t = getException();
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Client");
                alert.setHeaderText( t.getClass().getName() );
                alert.setContentText( t.getMessage() );
                alert.showAndWait();
            }
        };

        new Thread(task).start();

    }

    @FXML
    private void authClient() {
        if (!connected.get()) {
            connect();
        }
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
        channel.writeAndFlush(new ListRequest());
    }

    @FXML
    private void downloadFile() {
        channel.writeAndFlush(new FileRequest(getSelectedServerFile()));
    }

    @FXML
    private void sendFile() throws IOException {
        channel.writeAndFlush(new FileMessage(getSelectedLocalFile()));
//        try {
//            Path path = getSelectedLocalFile();
//            RandomAccessFile raf = new RandomAccessFile(path.toString(), "r");
//            channel.writeAndFlush(new ChunkedFile(raf));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    @FXML
    private void deleteLocalFile() throws IOException {
        Files.delete(getSelectedLocalFile());
        refreshLocalFiles();
    }

    @FXML
    private void deleteServerFile() {
        channel.writeAndFlush(new FileDelete(getSelectedServerFile()));
    }

    private Path getSelectedLocalFile() {
        return Paths.get(STORAGE + fileLocalTable.getSelectionModel().getSelectedItem().getFileName().getValue());
    }

    private String getSelectedServerFile() {
        return fileServerTable.getSelectionModel().getSelectedItem().getFileName().getValue();
    }

    private static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }

}