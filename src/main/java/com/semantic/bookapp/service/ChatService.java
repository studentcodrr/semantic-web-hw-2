package com.semantic.bookapp.service;

import com.semantic.bookapp.model.Book;
import com.semantic.bookapp.model.User;

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

    public String generateResponse(String userMessage, String pageContext, String bookId, String userId) {
        if (chatModel == null) {
            return "Error: no LLM";
        }

        try {
            if (isSearchQuery(userMessage)) {
                return handleSearchQuery(userMessage);
            }

            final Book currentBook = (bookId != null && !bookId.isEmpty())
                    ? rdfService.getBookById(bookId)
                    : null;

            List<Book> relevantBooks = (currentBook != null)
                    ? vectorService.searchBooksForUser(currentBook.getTitle(), userId, 3)
                    : vectorService.searchBooksForUser(userMessage, userId, 3);
            
                    if (relevantBooks == null) {
                relevantBooks = new ArrayList<>();
            }

            if (currentBook != null) {
                relevantBooks = relevantBooks.stream()
                        .filter(b -> !b.getId().equals(currentBook.getId()))
                        .collect(Collectors.toList());
            }

            if (relevantBooks.size() > 3) {
                relevantBooks = relevantBooks.subList(0, 3);
            }

            String context = vectorService.buildContext(relevantBooks);

            if (relevantBooks.isEmpty()) {
                context = "DATABASE STATUS: The database is currently empty for this specific query.";
            }

            User user = null;
            String userPreferences = "";
            String userInterests = "";

            if (userId != null && !userId.isEmpty()) {
                user = rdfService.getUserId(userId);

                if (user != null) {
                    String themePref = user.getPrefersTheme(); // e.g., "Fantasy"
                    String levelPref = user.getReadingLevel(); // e.g., "Intermediate"

                    if (themePref != null && !themePref.isEmpty()) {
                        userPreferences += "User prefers " + themePref + " books. ";
                    }
                    if (levelPref != null && !levelPref.isEmpty()) {
                        userPreferences += "User reading level is " + levelPref + ".";
                    }
                }
            }

            if (currentBook != null && !currentBook.getThemes().isEmpty()) {
                userInterests = "User looks at interested in "
                        + currentBook.getThemes().get(0) + " books.";
            }

            String prompt = buildPrompt(context, userMessage)
                    + "\nUSER PREFERENCES: " + userPreferences
                    + "\nUSER INTERESTS: " + userInterests;

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
                "### SYSTEM INSTRUCTIONS ###\n" +
                        "You are a strict RDF-based assistant. You have ZERO knowledge outside of the provided context.\n"
                        +
                        "1. Answer ONLY using the DATABASE provided below.\n" +
                        "2. If the user asks about a book NOT in the database, you MUST say: 'I'm sorry, that book is not in my library.'\n"
                        +
                        "3. Do NOT use your own training data or general knowledge.\n" +
                        "4. If the database says the author is 'Gigel', do NOT correct it to the real author.\n" +
                        "\n" +
                        "### DATABASE CONTEXT ###\n" +
                        "%s\n" +
                        "\n" +
                        "### USER MESSAGE ###\n" +
                        "%s\n" +
                        "\n" +
                        "### YOUR RESPONSE ###",
                context, userMessage);
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
        String[] patterns = { "author ", "by ", "written by " };
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
                    if (author.length() > 0)
                        author.append(" ");
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
                "Romance", "Adventure", "Horror", "Thriller");

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

    public List<String> generateConversationStarters(String pageType, String bookId, String userId) {
        List<String> starters = new ArrayList<>();
        User user = (userId != null) ? rdfService.getUserId(userId) : null;

        if ("book-detail".equals(pageType) && bookId != null) {
            Book book = rdfService.getBookById(bookId);
            if (book != null) {
                if (book.getTitle() != null) {
                    starters.add("Who is the author of " + book.getTitle() + "?");
                }
                if (book.getReadingLevel() != null) {
                    starters.add("What other books have " + book.getReadingLevel() + " level?");
                }
                if (!book.getThemes().isEmpty()) {
                    starters.add("What are similar " + book.getThemes().get(0) + " books?");
                }
            }
        } else if ("books".equals(pageType)) {
            if (user != null) {
                if (user.getPrefersTheme() != null) {
                    starters.add("What are more " + user.getPrefersTheme() + " books?");
                }
                if (user.getReadingLevel() != null) {
                    starters.add("What books have " + user.getReadingLevel() + " level?");
                }
                starters.add("Tell me more Science Fiction books");

            } else {
                starters.add("Show me all Science Fiction books");
                starters.add("Give me 3 random recomandations.");
            }

        } else {
            starters.add("What books do you have?");
            starters.add("Recommend a book for beginners");
            starters.add("What genres are available?");
        }

        return starters;
    }
}