package ru.mobydrake.common.utils;

import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileObj {
    private StringProperty fileName;
    private LongProperty size;

    public FileObj(Path path) {
        this.fileName = new SimpleStringProperty(path.getFileName().toString());
        try {
            this.size = new SimpleLongProperty(Files.size(path));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public FileObj(String name) {
        this.fileName = new SimpleStringProperty(name);
    }

    public StringProperty getFileName() {
        return fileName;
    }

    public LongProperty getSize() {
        return size;
    }
}
