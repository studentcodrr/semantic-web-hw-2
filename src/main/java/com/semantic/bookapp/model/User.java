package com.semantic.bookapp.model;

public class User {
    private String id;            
    private String name;         
    private String readingLevel;  
    private String prefersTheme;  

    public User() {}

    public User(String id, String name, String readingLevel, String prefersTheme) {
        this.id = id;
        this.name = name;
        this.readingLevel = readingLevel;
        this.prefersTheme = prefersTheme;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getReadingLevel() { return readingLevel; }
    public void setReadingLevel(String readingLevel) { this.readingLevel = readingLevel; }

    public String getPrefersTheme() { return prefersTheme; }
    public void setPrefersTheme(String prefersTheme) { this.prefersTheme = prefersTheme; }
}
