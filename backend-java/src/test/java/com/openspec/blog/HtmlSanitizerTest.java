package com.openspec.blog;

import com.openspec.blog.util.HtmlSanitizer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HtmlSanitizerTest {

    @Test
    void sanitize_removesScriptTags() {
        String input = "<script>alert('xss')</script><p>Hello</p>";
        String result = HtmlSanitizer.sanitize(input);

        assertFalse(result.contains("<script>"));
        assertTrue(result.contains("<p>Hello</p>"));
    }

    @Test
    void sanitize_removesOnEventHandlers() {
        String input = "<img src=x onerror=\"alert('xss')\">";
        String result = HtmlSanitizer.sanitize(input);

        assertFalse(result.contains("onerror"));
    }

    @Test
    void sanitize_removesJavascriptProtocol() {
        String input = "<a href=\"javascript:alert('xss')\">Click</a>";
        String result = HtmlSanitizer.sanitize(input);

        assertFalse(result.contains("javascript:"));
    }

    @Test
    void sanitize_preservesNormalHtml() {
        String input = "<h1>Title</h1><p>Paragraph</p>";
        String result = HtmlSanitizer.sanitize(input);

        assertEquals(input, result);
    }

    @Test
    void sanitize_handlesNull() {
        assertEquals("", HtmlSanitizer.sanitize(null));
    }

    @Test
    void sanitize_handlesEmptyString() {
        assertEquals("", HtmlSanitizer.sanitize(""));
    }
}
