package com.semantic.bookapp.model;

public class BookEmbedding {
    private Book book;
    private float[] embedding;
    private String textRepresentation;

    public BookEmbedding(Book book, float[] embedding, String textRepresentation) {
        this.book = book;
        this.embedding = embedding;
        this.textRepresentation = textRepresentation;
    }

    public Book getBook() {
        return book;
    }

    public void setBook(Book book) {
        this.book = book;
    }

    public float[] getEmbedding() {
        return embedding;
    }

    public void setEmbedding(float[] embedding) {
        this.embedding = embedding;
    }

    public String getTextRepresentation() {
        return textRepresentation;
    }

    public void setTextRepresentation(String textRepresentation) {
        this.textRepresentation = textRepresentation;
    }
}