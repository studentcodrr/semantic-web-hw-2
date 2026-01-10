package com.semantic.bookapp.service;

import com.semantic.bookapp.model.Book;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ChatService {

    @Autowired
    private VectorService vectorService;

    @Autowired
    private RdfService rdfService;

    @Value("${google.api.key}")
    private String apiKey;

    @Value("${llm.model:gemini-2.5-flash}")
    private String modelName;

    private ChatLanguageModel chatModel;

    @PostConstruct
    public void initialize() {
        System.out.println("Start...");

        if (apiKey == null || apiKey.equals("YOUR_GOOGLE_API_KEY_HERE")) {
            System.err.println("ERROR: No Google API key configured");
            return;
        }

        chatModel = GoogleAiGeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .temperature(0.7)
                .build();

        System.out.println("LLM initialized: " + modelName);
    }

    public String generateResponse(String userMessage, String pageContext, String bookId) {
        if (chatModel == null) {
            return "Error: LLM not configured.";
        }

        try {
            if (isSearchQuery(userMessage)) {
                return handleSearchQuery(userMessage);
            }

            List<Book> relevantBooks = vectorService.searchSimilarBooks(userMessage, 3);

            if (bookId != null && !bookId.isEmpty()) {
                Book currentBook = rdfService.getBookById(bookId);
                if (currentBook != null && !relevantBooks.contains(currentBook)) {
                    relevantBooks.add(0, currentBook);
                    if (relevantBooks.size() > 3) {
                        relevantBooks = relevantBooks.subList(0, 3);
                    }
                }
            }

            String context = vectorService.buildContext(relevantBooks);
            String prompt = buildPrompt(context, userMessage);

            String response = chatModel.generate(prompt);
            return response;

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            return "Error processing your request";
        }
    }

    private String buildPrompt(String context, String userMessage) {
        return String.format(
                "You are a helpful book recommendation assistant. Answer questions based ONLY on the book data provided below.\n\n" +
                        "%s\n" +
                        "INSTRUCTIONS:\n" +
                        "- Only use information from the books listed above\n" +
                        "- If a book's author is listed as 'Something', use 'Something' (not the real author)\n" +
                        "- If the answer isn't in the database, say 'I don't have that information in my database'\n" +
                        "- Be concise and helpful\n" +
                        "- Don't mention that you're looking at a database or context\n\n" +
                        "USER QUESTION: %s\n\n" +
                        "ANSWER:",
                context,
                userMessage
        );
    }

    private boolean isSearchQuery(String message) {
        String lower = message.toLowerCase();
        return (lower.contains("author") || lower.contains("written by")) &&
                (lower.contains("theme") || lower.contains("genre"));
    }

    private String handleSearchQuery(String message) {
        String author = extractAuthor(message);
        String theme = extractTheme(message);

        List<Book> results = searchByAuthorAndTheme(author, theme);

        if (results.isEmpty()) {
            return "I couldn't find any books matching those criteria.";
        }

        StringBuilder response = new StringBuilder();
        response.append("Based on your search, I found:\n\n");

        for (Book book : results) {
            response.append("- ").append(book.getTitle());
            if (book.getAuthor() != null) {
                response.append(" by ").append(book.getAuthor());
            }
            response.append("\n");
        }

        return response.toString().trim();
    }

    private String extractAuthor(String message) {
        String[] patterns = {"author ", "by ", "written by "};
        for (String pattern : patterns) {
            int index = message.toLowerCase().indexOf(pattern);
            if (index != -1) {
                String afterPattern = message.substring(index + pattern.length());
                String[] words = afterPattern.split(" ");

                StringBuilder author = new StringBuilder();
                for (int i = 0; i < Math.min(3, words.length); i++) {
                    String word = words[i].replaceAll("[^a-zA-Z ]", "").trim();
                    if (word.isEmpty() || word.equalsIgnoreCase("and") || word.equalsIgnoreCase("the")) {
                        break;
                    }
                    if (author.length() > 0) author.append(" ");
                    author.append(word);
                }
                return author.toString();
            }
        }
        return "";
    }

    private String extractTheme(String message) {
        List<String> knownThemes = Arrays.asList(
                "Science Fiction", "Fantasy", "Mystery", "Murder",
                "Romance", "Adventure", "Horror", "Thriller"
        );

        for (String theme : knownThemes) {
            if (message.toLowerCase().contains(theme.toLowerCase())) {
                return theme;
            }
        }
        return "";
    }

    private List<Book> searchByAuthorAndTheme(String author, String theme) {
        List<Book> allBooks = rdfService.getAllBooks();

        return allBooks.stream()
                .filter(book -> {
                    boolean authorMatch = author.isEmpty() ||
                            (book.getAuthor() != null &&
                                    book.getAuthor().toLowerCase().contains(author.toLowerCase()));

                    boolean themeMatch = theme.isEmpty() ||
                            book.getThemes().stream()
                                    .anyMatch(t -> t.toLowerCase().contains(theme.toLowerCase()));

                    return authorMatch && themeMatch;
                })
                .collect(Collectors.toList());
    }

    public List<String> generateConversationStarters(String pageType, String bookId) {
        List<String> starters = new ArrayList<>();

        if ("book-detail".equals(pageType) && bookId != null) {
            Book book = rdfService.getBookById(bookId);
            if (book != null) {
                starters.add("Tell me more about " + book.getTitle());

                if (book.getAuthor() != null) {
                    starters.add("Who is " + book.getAuthor() + "?");
                }

                if (!book.getThemes().isEmpty()) {
                    String firstTheme = book.getThemes().get(0);
                    starters.add("What are similar " + firstTheme + " books?");
                }
            }
        } else if ("books".equals(pageType)) {
            starters.add("What book would you recommend for me?");
            starters.add("Show me all Science Fiction books");
            starters.add("What are the most advanced reading level books?");
        } else {
            starters.add("What books do you have?");
            starters.add("Recommend a book for beginners");
            starters.add("What genres are available?");
        }

        return starters;
    }
}