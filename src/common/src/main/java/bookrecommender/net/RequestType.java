package bookrecommender.net;

public enum RequestType {
    PING,

    // SEARCH
    SEARCH_BY_TITLE,
    SEARCH_BY_AUTHOR,
    SEARCH_BY_AUTHOR_YEAR,
    GET_BOOK_BY_ID,


    // AUTH
    LOGIN,
    REGISTER,
    LOGOUT,
    CHANGE_PASSWORD,
    UPDATE_EMAIL,
    DELETE_ACCOUNT,

    // REVIEWS
    SAVE_REVIEW,
    GET_REVIEWS_BY_BOOK,
    LIST_REVIEWS_BY_USER,
    DELETE_REVIEW,

    // SUGGESTIONS
    GET_SUGGESTIONS_BY_BOOK,
    LIST_SUGGESTIONS_BY_USER,
    SAVE_SUGGESTION,          // salva l'intera Suggestion (userid+bookId + lista max 3)
    DELETE_SUGGESTION,        // elimina i consigli di (userid, bookId)
    
      

    // LIBRARIES
    LIST_LIBRARIES_BY_USER,
    SAVE_LIBRARY,             // salva la libreria (userid+nome + set bookIds)
    DELETE_LIBRARY
}
