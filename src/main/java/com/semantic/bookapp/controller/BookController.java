package com.semantic.bookapp.controller;

import com.semantic.bookapp.model.Book;
import com.semantic.bookapp.service.RdfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
public class BookController {

    @Autowired
    private RdfService rdfService;

    @GetMapping("/books")
    public String listBooks(Model model) {
        List<Book> books = rdfService.getAllBooks();
        model.addAttribute("books", books);
        return "books";
    }

    // View individual book
    @GetMapping("/book/{id}")
    public String viewBook(@PathVariable String id, Model model) {
        Book book = rdfService.getBookById(id);

        if (book == null) {
            model.addAttribute("message", "Book not found");
            return "error";
        }

        model.addAttribute("book", book);
        return "book-detail";
    }

    // Show add/edit book form
    @GetMapping("/manage-book")
    public String manageBook(@RequestParam(required = false) String id, Model model) {
        if (id != null && !id.isEmpty()) {
            Book book = rdfService.getBookById(id);
            if (book != null) {
                model.addAttribute("book", book);
            }
        }
        return "manage-book";
    }

    // Save book
    @PostMapping("/save-book")
    public String saveBook(
            @RequestParam String id,
            @RequestParam String title,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) List<String> themes,
            @RequestParam String readingLevel) {

        rdfService.saveBook(id.replaceAll("\\s+", ""), title, author, themes, readingLevel);
        return "redirect:/books";
    }
}