package ie.griffith.tasks.service;

import ie.griffith.tasks.model.Priority;

/**
 * What the user chose in the search/filter/sort controls. null means "any".
 * @param text       matches title or description (case-insensitive)
 * @param category   exact category, or null for all
 * @param priority   exact priority, or null for all
 * @param hideDone   true to hide completed tasks
 * @param sortBy     how to order the results
 */
public record TaskFilter(String text, String category, Priority priority, boolean hideDone, SortBy sortBy) {

    public enum SortBy { DEADLINE, PRIORITY, TITLE, CATEGORY }

    /** No filtering, sorted by deadline. */
    public static TaskFilter none() {
        return new TaskFilter(null, null, null, false, SortBy.DEADLINE);
    }
}
