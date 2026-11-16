package com.harvesthub.desktop.util;

import java.util.List;

/** Client-side paging for tables. Pages are 1-based. */
public final class Pager<T> {

    private final int pageSize;
    private List<T> items = List.of();
    private int page = 1;

    public Pager(int pageSize) {
        if (pageSize < 1) throw new IllegalArgumentException("pageSize must be >= 1");
        this.pageSize = pageSize;
    }

    public void setItems(List<T> items) {
        this.items = items == null ? List.of() : List.copyOf(items);
        page = Math.min(page, pages());
    }

    public int pages() {
        return Math.max(1, (items.size() + pageSize - 1) / pageSize);
    }

    public int page() {
        return page;
    }

    public void go(int p) {
        page = Math.max(1, Math.min(p, pages()));
    }

    public boolean hasPrev() {
        return page > 1;
    }

    public boolean hasNext() {
        return page < pages();
    }

    public List<T> current() {
        int from = (page - 1) * pageSize;
        return items.subList(Math.min(from, items.size()), Math.min(from + pageSize, items.size()));
    }

    public int total() {
        return items.size();
    }

    /** "Showing 11–20 of 42" / "No results". */
    public String summary() {
        if (items.isEmpty()) return "No results";
        int from = (page - 1) * pageSize + 1;
        int to = Math.min(page * pageSize, items.size());
        return "Showing " + from + "–" + to + " of " + items.size();
    }
}
