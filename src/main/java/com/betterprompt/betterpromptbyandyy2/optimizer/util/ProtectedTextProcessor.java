package com.betterprompt.betterpromptbyandyy2.optimizer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies text transformations only outside Markdown code spans.
 */
public final class ProtectedTextProcessor {

    private static final Pattern PROTECTED_PATTERN = Pattern.compile(
            "```[\\s\\S]*?```|`[^`\\r\\n]*`"
    );

    private ProtectedTextProcessor() {
    }

    public static String transformOutsideMarkdownCode(String text, UnaryOperator<String> transformer) {
        if (text == null || text.isEmpty() || transformer == null) {
            return text;
        }

        Matcher matcher = PROTECTED_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder(text.length());
        int cursor = 0;
        while (matcher.find()) {
            if (cursor < matcher.start()) {
                result.append(transformer.apply(text.substring(cursor, matcher.start())));
            }
            result.append(matcher.group());
            cursor = matcher.end();
        }

        if (cursor < text.length()) {
            result.append(transformer.apply(text.substring(cursor)));
        }
        return result.toString();
    }

    public static List<Segment> splitIntoSegments(String input) {
        if (input == null || input.isEmpty()) {
            return List.of(new Segment(input == null ? "" : input, false));
        }

        List<Segment> segments = new ArrayList<>();
        Matcher matcher = PROTECTED_PATTERN.matcher(input);
        int cursor = 0;

        while (matcher.find()) {
            if (cursor < matcher.start()) {
                segments.add(new Segment(input.substring(cursor, matcher.start()), false));
            }
            segments.add(new Segment(matcher.group(), true));
            cursor = matcher.end();
        }

        if (cursor < input.length()) {
            segments.add(new Segment(input.substring(cursor), false));
        }
        if (segments.isEmpty()) {
            return List.of(new Segment(input, false));
        }
        return segments;
    }

    public static final class Segment {
        private final String text;
        private final boolean protectedSegment;

        public Segment(String text, boolean protectedSegment) {
            this.text = text;
            this.protectedSegment = protectedSegment;
        }

        public String getText() {
            return text;
        }

        public boolean isProtectedSegment() {
            return protectedSegment;
        }
    }

}
