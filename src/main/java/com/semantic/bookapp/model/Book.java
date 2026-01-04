package com.semantic.bookapp.model;

import java.util.ArrayList;
import java.util.List;

public class Book {
    private String id;
    private String uri;
    private String title;
    private String author;
    private List<String> themes;
    private String readingLevel;

    public Book() {
        this.themes = new ArrayList<>();
    }

    public Book(String id, String title) {
        this.id = id;
        this.title = title;
        this.themes = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public List<String> getThemes() {
        return themes;
    }

    public void setThemes(List<String> themes) {
        this.themes = themes;
    }

    public void addTheme(String theme) {
        this.themes.add(theme);
    }

    public String getReadingLevel() {
        return readingLevel;
    }

    public void setReadingLevel(String readingLevel) {
        this.readingLevel = readingLevel;
    }
}