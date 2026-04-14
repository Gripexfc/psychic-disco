package com.openspec.blog.util;

import java.util.regex.Pattern;

public class HtmlSanitizer {

    private static final Pattern SCRIPT_PATTERN = Pattern.compile(
            "<script[\\s\\S]*?>[\\s\\S]*?</script>",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern ON_EVENT_PATTERN = Pattern.compile(
            "\\son\\w+=\"[^\"]*\"",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern JAVASCRIPT_PROTOCOL_PATTERN = Pattern.compile(
            "\\bjavascript:",
            Pattern.CASE_INSENSITIVE
    );

    public static String sanitize(String input) {
        if (input == null) {
            return "";
        }
        String result = input;
        result = SCRIPT_PATTERN.matcher(result).replaceAll("");
        result = ON_EVENT_PATTERN.matcher(result).replaceAll("");
        result = JAVASCRIPT_PROTOCOL_PATTERN.matcher(result).replaceAll("");
        return result;
    }
}
