package com.semantic.bookapp.service;

import com.semantic.bookapp.model.Book;
import org.apache.jena.rdf.model.*;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RdfService {
    private Model model;
    private static final String RDF_FILE_PATH = "src/main/resources/data/books.rdf";
    private static final String BOOK_NS = "http://example.org/books#";
    private static final String USER_NS = "http://example.org/users#";

    @PostConstruct
    public void init() {
        model = ModelFactory.createDefaultModel();
        loadRdfFile();
    }

    public void loadRdfFile() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("data/books.rdf")) {
            if (in != null) {
                model.read(in, null, "RDF/XML");
                System.out.println("RDF file loaded successfully from classpath");
                System.out.println("Model contains " + model.size() + " statements");

                //DEBUG: all books
                Resource bookClass = model.createResource(BOOK_NS + "Book");
                ResIterator iter = model.listSubjectsWithProperty(RDF.type, bookClass);
                int bookCount = 0;
                while (iter.hasNext()) {
                    Resource book = iter.nextResource();
                    bookCount++;
                    System.out.println("- Found book: " + book.getURI());
                }
                System.out.println("Total books found: " + bookCount);
                return;
            }
        } catch (IOException e) {
            System.err.println("x Error loading RDF from classpath: " + e.getMessage());
        }

        File file = new File(RDF_FILE_PATH);
        System.out.println("Trying to load from file system: " + file.getAbsolutePath());

        if (file.exists()) {
            try (InputStream in = new FileInputStream(file)) {
                model.read(in, null, "RDF/XML");
                System.out.println("RDF file loaded from file system");
                System.out.println("Model contains " + model.size() + " statements");
            } catch (IOException e) {
                System.err.println("x Error loading RDF file: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            System.err.println("x No RDF file found: " + file.getAbsolutePath());
            System.err.println("x Please place books.rdf in src/main/resources/data/");
        }
    }

    //Save model
    public void saveRdfFile() {
        File file = new File(RDF_FILE_PATH);
        file.getParentFile().mkdirs();

        try (OutputStream out = new FileOutputStream(file)) {
            model.write(out, "RDF/XML");
            System.out.println("RDF file saved successfully to: " + file.getAbsolutePath());
            System.out.println("Model now contains " + model.size() + " statements");
        } catch (IOException e) {
            System.err.println("✗ Error saving RDF file: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public List<Map<String, String>> parseUploadedRdf(InputStream inputStream) {
        List<Map<String, String>> triples = new ArrayList<>();
        Model tempModel = ModelFactory.createDefaultModel();

        try {
            tempModel.read(inputStream, null, "RDF/XML");

            StmtIterator iter = tempModel.listStatements();
            while (iter.hasNext()) {
                Statement stmt = iter.nextStatement();
                Map<String, String> triple = new HashMap<>();

                triple.put("subject", stmt.getSubject().toString());
                triple.put("predicate", stmt.getPredicate().toString());

                RDFNode object = stmt.getObject();
                if (object.isLiteral()) {
                    triple.put("object", object.asLiteral().getString());
                    triple.put("objectType", "Literal");
                } else {
                    triple.put("object", object.toString());
                    triple.put("objectType", "NamedNode");
                }

                triples.add(triple);
            }
        } catch (Exception e) {
            System.err.println("Error parsing RDF: " + e.getMessage());
        }

        return triples;
    }

    public List<Book> getAllBooks() {
        List<Book> books = new ArrayList<>();
        Resource bookClass = model.createResource(BOOK_NS + "Book");

        System.out.println("DEBUG: Looking for books with type: " + bookClass.getURI());
        System.out.println("DEBUG: Model size: " + model.size());

        ResIterator iter = model.listSubjectsWithProperty(RDF.type, bookClass);
        while (iter.hasNext()) {
            Resource bookResource = iter.nextResource();
            System.out.println("DEBUG: Processing book: " + bookResource.getURI());
            Book book = extractBookFromResource(bookResource);
            books.add(book);
        }

        System.out.println("DEBUG: Total books retrieved: " + books.size());
        return books;
    }

    public Book getBookById(String id) {
        Resource bookResource = model.createResource(BOOK_NS + id);

        if (model.contains(bookResource, RDF.type, model.createResource(BOOK_NS + "Book"))) {
            return extractBookFromResource(bookResource);
        }

        return null;
    }

    private Book extractBookFromResource(Resource bookResource) {
        Book book = new Book();
        String uri = bookResource.getURI();
        book.setUri(uri);
        book.setId(uri.substring(uri.indexOf('#') + 1));

        Statement titleStmt = bookResource.getProperty(model.createProperty(BOOK_NS + "title"));
        if (titleStmt != null) {
            book.setTitle(titleStmt.getString());
        }

        Statement authorStmt = bookResource.getProperty(model.createProperty(BOOK_NS + "author"));
        if (authorStmt != null) {
            book.setAuthor(authorStmt.getString());
        }

        StmtIterator themeIter = bookResource.listProperties(model.createProperty(BOOK_NS + "belongsToTheme"));
        while (themeIter.hasNext()) {
            Statement themeStmt = themeIter.nextStatement();
            Resource themeResource = themeStmt.getResource();
            String themeUri = themeResource.getURI();
            String themeName = themeUri.substring(themeUri.indexOf('#') + 1);

            Statement labelStmt = themeResource.getProperty(RDFS.label);
            if (labelStmt != null) {
                book.addTheme(labelStmt.getString());
            } else {
                book.addTheme(themeName);
            }
        }

        Statement levelStmt = bookResource.getProperty(model.createProperty(BOOK_NS + "suitableFor"));
        if (levelStmt != null) {
            String levelUri = levelStmt.getResource().getURI();
            book.setReadingLevel(levelUri.substring(levelUri.indexOf('#') + 1));
        }

        return book;
    }

    public void saveBook(String id, String title, String author, List<String> themes, String readingLevel) {
        Resource bookResource = model.createResource(BOOK_NS + id);

        model.removeAll(bookResource, null, null);

        bookResource.addProperty(RDF.type, model.createResource(BOOK_NS + "Book"));

        bookResource.addProperty(
                model.createProperty(BOOK_NS + "title"),
                model.createLiteral(title)
        );

        if (author != null && !author.trim().isEmpty()) {
            bookResource.addProperty(
                    model.createProperty(BOOK_NS + "author"),
                    model.createLiteral(author)
            );
        }

        if (themes != null) {
            for (String theme : themes) {
                bookResource.addProperty(
                        model.createProperty(BOOK_NS + "belongsToTheme"),
                        model.createResource(BOOK_NS + theme)
                );
            }
        }

        bookResource.addProperty(
                model.createProperty(BOOK_NS + "suitableFor"),
                model.createResource(BOOK_NS + readingLevel)
        );

        saveRdfFile();
    }

    public boolean bookExists(String id) {
        Resource bookResource = model.createResource(BOOK_NS + id);
        return model.contains(bookResource, RDF.type, model.createResource(BOOK_NS + "Book"));
    }
}