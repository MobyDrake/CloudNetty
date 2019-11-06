package ru.mobydrake.common;

public class FileRequest extends AbstractMessage {
    private String name;

    public FileRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
