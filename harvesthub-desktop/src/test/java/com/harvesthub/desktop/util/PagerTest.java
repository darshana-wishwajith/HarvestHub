package com.harvesthub.desktop.util;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class PagerTest {

    @Test
    void pagesThroughItems() {
        Pager<Integer> p = new Pager<>(10);
        p.setItems(IntStream.rangeClosed(1, 25).boxed().toList());
        assertEquals(3, p.pages());
        assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), p.current());
        assertEquals("Showing 1–10 of 25", p.summary());
        assertFalse(p.hasPrev());
        p.go(3);
        assertEquals(List.of(21, 22, 23, 24, 25), p.current());
        assertEquals("Showing 21–25 of 25", p.summary());
        assertFalse(p.hasNext());
        p.go(99);
        assertEquals(3, p.page(), "clamped to the last page");
    }

    @Test
    void shrinkingTheListKeepsAValidPage() {
        Pager<String> p = new Pager<>(2);
        p.setItems(List.of("a", "b", "c", "d", "e"));
        p.go(3);
        p.setItems(List.of("a"));
        assertEquals(1, p.page());
        p.setItems(List.of());
        assertEquals("No results", p.summary());
        assertTrue(p.current().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> new Pager<>(0));
    }
}
