package com.semantic.bookapp.service;

import com.semantic.bookapp.model.Book;
import com.semantic.bookapp.model.BookEmbedding;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VectorService {

    @Autowired
    private RdfService rdfService;

    private EmbeddingModel embeddingModel;
    private List<BookEmbedding> bookEmbeddings;

    @PostConstruct
    public void initialize() {
        System.out.println("Initializing Vector Database...");

        embeddingModel = new AllMiniLmL6V2EmbeddingModel();

        bookEmbeddings = new ArrayList<>();
        List<Book> allBooks = rdfService.getAllBooks();

        for (Book book : allBooks) {
            String textRepresentation = createTextRepresentation(book);
            Embedding embedding = embeddingModel.embed(textRepresentation).content();

            BookEmbedding bookEmbedding = new BookEmbedding(
                    book,
                    embedding.vector(),
                    textRepresentation
            );
            bookEmbeddings.add(bookEmbedding);

            System.out.println("✓ Embedded: " + book.getTitle());
        }

        System.out.println("✓ Vector Database initialized with " + bookEmbeddings.size() + " books");
    }

    private String createTextRepresentation(Book book) {
        StringBuilder sb = new StringBuilder();

        sb.append("Title: ").append(book.getTitle()).append(". ");

        if (book.getAuthor() != null) {
            sb.append("Author: ").append(book.getAuthor()).append(". ");
        }

        if (!book.getThemes().isEmpty()) {
            sb.append("Themes: ").append(String.join(", ", book.getThemes())).append(". ");
        }

        if (book.getReadingLevel() != null) {
            sb.append("Reading Level: ").append(book.getReadingLevel()).append(".");
        }

        return sb.toString();
    }

    public List<Book> searchSimilarBooks(String query, int limit) {
        if (bookEmbeddings.isEmpty()) {
            return new ArrayList<>();
        }

        Embedding queryEmbedding = embeddingModel.embed(query).content();
        float[] queryVector = queryEmbedding.vector();

        List<ScoredBook> scoredBooks = new ArrayList<>();
        for (BookEmbedding bookEmbedding : bookEmbeddings) {
            double similarity = cosineSimilarity(queryVector, bookEmbedding.getEmbedding());
            scoredBooks.add(new ScoredBook(bookEmbedding.getBook(), similarity));
        }

        scoredBooks.sort((a, b) -> Double.compare(b.score, a.score));

        return scoredBooks.stream()
                .limit(limit)
                .map(sb -> sb.book)
                .collect(Collectors.toList());
    }

    private double cosineSimilarity(float[] vectorA, float[] vectorB) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    public String buildContext(List<Book> books) {
        StringBuilder context = new StringBuilder();
        context.append("BOOKS IN DATABASE:\n\n");

        for (Book book : books) {
            context.append("- Title: ").append(book.getTitle()).append("\n");

            if (book.getAuthor() != null) {
                context.append("  Author: ").append(book.getAuthor()).append("\n");
            }

            if (!book.getThemes().isEmpty()) {
                context.append("  Themes: ").append(String.join(", ", book.getThemes())).append("\n");
            }

            if (book.getReadingLevel() != null) {
                context.append("  Reading Level: ").append(book.getReadingLevel()).append("\n");
            }

            context.append("\n");
        }

        return context.toString();
    }

    public List<String> getAllBookTexts() {
        return bookEmbeddings.stream()
                .map(BookEmbedding::getTextRepresentation)
                .collect(Collectors.toList());
    }

    public void refresh() {
        initialize();
    }

    private static class ScoredBook {
        Book book;
        double score;

        ScoredBook(Book book, double score) {
            this.book = book;
            this.score = score;
        }
    }
}