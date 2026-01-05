package bookrecommender.net;

import java.io.Serializable;

public class Request implements Serializable {
    public final RequestType type;
    public final Object payload;
    public final String token;

    public Request(RequestType type, Object payload, String token) {
        this.type = type;
        this.payload = payload;
        this.token = token;
    }

    public static Request ping() {
        return new Request(RequestType.PING, null, null);
    }

    public static Request searchByTitle(String titolo) {
        return new Request(RequestType.SEARCH_BY_TITLE, titolo, null);
    }

    public static Request searchByAuthor(String autore) {
        return new Request(RequestType.SEARCH_BY_AUTHOR, autore, null);
    }

    public static Request login(String userid, String passwordHash) {
        return new Request(RequestType.LOGIN, new String[]{userid, passwordHash}, null);
    }

    public static Request register(bookrecommender.model.User u) {
        return new Request(RequestType.REGISTER, u, null);
    }

    public static Request logout() {
        return new Request(RequestType.LOGOUT, null, null);
    }

        // ===== REVIEWS =====
    public static Request saveReview(bookrecommender.model.Review r) {
        return new Request(RequestType.SAVE_REVIEW, r, null);
    }

    public static Request getReviewsByBook(int bookId) {
        return new Request(RequestType.GET_REVIEWS_BY_BOOK, bookId, null);
    }

    public static Request listReviewsByUser(String userid) {
        return new Request(RequestType.LIST_REVIEWS_BY_USER, userid, null);
    }

    public static Request deleteReview(String userid, int bookId) {
        return new Request(RequestType.DELETE_REVIEW, new Object[]{userid, bookId}, null);
    }

    // ===== SUGGESTIONS =====
    public static Request getSuggestionsByBook(int bookId) {
        return new Request(RequestType.GET_SUGGESTIONS_BY_BOOK, bookId, null);
    }

    public static Request listSuggestionsByUser(String userid) {
        return new Request(RequestType.LIST_SUGGESTIONS_BY_USER, userid, null);
    }

    public static Request saveSuggestion(bookrecommender.model.Suggestion s) {
        return new Request(RequestType.SAVE_SUGGESTION, s, null);
    }

    public static Request deleteSuggestion(String userid, int bookId) {
        return new Request(RequestType.DELETE_SUGGESTION, new Object[]{userid, bookId}, null);
    }

    // ===== LIBRARIES =====
    public static Request listLibrariesByUser(String userid) {
        return new Request(RequestType.LIST_LIBRARIES_BY_USER, userid, null);
    }

    public static Request saveLibrary(bookrecommender.model.Library lib) {
        return new Request(RequestType.SAVE_LIBRARY, lib, null);
    }

    public static Request deleteLibrary(String userid, String nome) {
        return new Request(RequestType.DELETE_LIBRARY, new Object[]{userid, nome}, null);
    }

    public static Request searchByAuthorYear(String author, int year, int limit) {
        return new Request(RequestType.SEARCH_BY_AUTHOR_YEAR, new Object[]{author, year, limit}, null);
    }

    // overload di compatibilità: se qualcuno chiama ancora (author, year)
    public static Request searchByAuthorYear(String author, int year) {
        return searchByAuthorYear(author, year, 50);
    }

    public static Request getBookById(int bookId) {
        return new Request(RequestType.GET_BOOK_BY_ID, new Object[]{bookId}, null);
    }

        public static Request deleteAccount(String userid) {
        return new Request(RequestType.DELETE_ACCOUNT, userid, null);
    }


}
