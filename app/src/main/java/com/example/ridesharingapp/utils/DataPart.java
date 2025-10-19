package com.example.ridesharingapp.utils;

public class DataPart {
    private String fileName;
    private byte[] content;
    private String type;

    /**
     * Default constructor.
     */
    public DataPart() {
    }

    /**
     * Parameterized constructor.
     * @param fileName The name of the file to be sent to the server.
     * @param content The byte array content of the file.
     * @param type The MIME type of the content (e.g., "image/jpeg").
     */
    public DataPart(String fileName, byte[] content, String type) {
        this.fileName = fileName;
        this.content = content;
        this.type = type;
    }

    // Getters and Setters

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
