package ru.mobydrake.common.messages;

public class FileDelete extends AbstractMessage {
    private String fileName;

    public FileDelete(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
