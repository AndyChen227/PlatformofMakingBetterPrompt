package com.betterprompt.betterpromptbyandyy2.optimizer.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Applies text transformations only outside protected text spans.
 */
public final class ProtectedTextProcessor {

    private static final Pattern MARKDOWN_CODE_PATTERN = Pattern.compile(
            "```[\\s\\S]*?```|`[^`\\r\\n]*`"
    );

    private ProtectedTextProcessor() {
    }

    public static String transformOutsideMarkdownCode(String text, UnaryOperator<String> transformer) {
        if (text == null || text.isEmpty() || transformer == null) {
            return text;
        }

        StringBuilder result = new StringBuilder(text.length());
        List<Segment> segments = splitIntoSegments(text);
        for (int i = 0; i < segments.size(); i++) {
            Segment segment = segments.get(i);
            if (segment.isProtectedSegment()) {
                result.append(segment.getText());
            } else {
                String transformed = transformer.apply(segment.getText());
                result.append(restoreBoundaryWhitespace(segment.getText(), transformed, segments, i));
            }
        }
        return result.toString();
    }

    public static List<Segment> splitIntoSegments(String input) {
        if (input == null || input.isEmpty()) {
            return List.of(new Segment(input == null ? "" : input, false));
        }

        List<Segment> segments = new ArrayList<>();
        List<Range> protectedRanges = findProtectedRanges(input);
        int cursor = 0;

        for (Range range : protectedRanges) {
            if (cursor < range.start()) {
                segments.add(new Segment(input.substring(cursor, range.start()), false));
            }
            segments.add(new Segment(input.substring(range.start(), range.end()), true));
            cursor = range.end();
        }

        if (cursor < input.length()) {
            segments.add(new Segment(input.substring(cursor), false));
        }
        if (segments.isEmpty()) {
            return List.of(new Segment(input, false));
        }
        return segments;
    }

    private static String restoreBoundaryWhitespace(
            String original,
            String transformed,
            List<Segment> segments,
            int segmentIndex
    ) {
        if (transformed == null || transformed.isEmpty()) {
            return transformed;
        }

        String result = transformed;
        if (segmentIndex > 0
                && segments.get(segmentIndex - 1).isProtectedSegment()
                && startsWithWhitespace(original)
                && !startsWithWhitespace(result)) {
            result = boundaryWhitespacePrefix(original) + result;
        }
        if (segmentIndex + 1 < segments.size()
                && segments.get(segmentIndex + 1).isProtectedSegment()
                && endsWithWhitespace(original)
                && !endsWithWhitespace(result)) {
            result = result + boundaryWhitespaceSuffix(original);
        }
        return result;
    }

    private static boolean startsWithWhitespace(String text) {
        return text != null && !text.isEmpty() && Character.isWhitespace(text.charAt(0));
    }

    private static boolean endsWithWhitespace(String text) {
        return text != null && !text.isEmpty() && Character.isWhitespace(text.charAt(text.length() - 1));
    }

    private static String boundaryWhitespacePrefix(String text) {
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            if (text.charAt(index) == '\r' || text.charAt(index) == '\n') {
                return leadingWhitespace(text);
            }
            index++;
        }
        return " ";
    }

    private static String boundaryWhitespaceSuffix(String text) {
        int index = text.length() - 1;
        while (index >= 0 && Character.isWhitespace(text.charAt(index))) {
            if (text.charAt(index) == '\r' || text.charAt(index) == '\n') {
                return trailingWhitespace(text);
            }
            index--;
        }
        return " ";
    }

    private static String leadingWhitespace(String text) {
        int index = 0;
        while (index < text.length() && Character.isWhitespace(text.charAt(index))) {
            index++;
        }
        return text.substring(0, index);
    }

    private static String trailingWhitespace(String text) {
        int index = text.length();
        while (index > 0 && Character.isWhitespace(text.charAt(index - 1))) {
            index--;
        }
        return text.substring(index);
    }

    private static List<Range> findProtectedRanges(String input) {
        List<Range> ranges = new ArrayList<>();
        Matcher matcher = MARKDOWN_CODE_PATTERN.matcher(input);
        while (matcher.find()) {
            ranges.add(new Range(matcher.start(), matcher.end()));
        }

        ranges.addAll(findQuotedTextRanges(input, ranges));
        return mergeRanges(ranges);
    }

    private static List<Range> findQuotedTextRanges(String input, List<Range> codeRanges) {
        List<Range> quotedRanges = new ArrayList<>();
        int codeIndex = 0;
        QuoteState quote = null;

        for (int i = 0; i < input.length(); i++) {
            if (codeIndex < codeRanges.size() && i >= codeRanges.get(codeIndex).end()) {
                codeIndex++;
            }
            if (codeIndex < codeRanges.size() && i == codeRanges.get(codeIndex).start()) {
                i = codeRanges.get(codeIndex).end() - 1;
                continue;
            }

            char current = input.charAt(i);
            if (quote == null) {
                Character close = closingQuoteForOpening(input, i, current);
                if (close != null) {
                    quote = new QuoteState(i, close);
                }
                continue;
            }

            if (current == quote.closeQuote()
                    && isValidClosingQuote(input, i, current)
                    && !isEscaped(input, i)) {
                quotedRanges.add(new Range(quote.start(), i + 1));
                quote = null;
            }
        }

        return quotedRanges;
    }

    private static Character closingQuoteForOpening(String input, int index, char current) {
        if (isEscaped(input, index)) {
            return null;
        }
        if (current == '"') {
            return isValidOpeningQuote(input, index) ? '"' : null;
        }
        if (current == '\'') {
            return isValidOpeningQuote(input, index) && !isApostrophe(input, index) ? '\'' : null;
        }
        if (current == '\u201c') {
            return '\u201d';
        }
        if (current == '\u2018') {
            return '\u2019';
        }
        return null;
    }

    private static boolean isValidOpeningQuote(String input, int index) {
        return hasNonWhitespaceAfter(input, index)
                && (index == 0 || Character.isWhitespace(input.charAt(index - 1))
                || isOpeningBoundary(input.charAt(index - 1)));
    }

    private static boolean isValidClosingQuote(String input, int index, char current) {
        if (current == '\'' && isApostrophe(input, index)) {
            return false;
        }
        return index > 0
                && !Character.isWhitespace(input.charAt(index - 1))
                && (index == input.length() - 1 || Character.isWhitespace(input.charAt(index + 1))
                || isClosingBoundary(input.charAt(index + 1)));
    }

    private static boolean hasNonWhitespaceAfter(String input, int index) {
        return index + 1 < input.length() && !Character.isWhitespace(input.charAt(index + 1));
    }

    private static boolean isOpeningBoundary(char c) {
        return c == '(' || c == '[' || c == '{' || c == '<'
                || c == ':' || c == '=' || c == '-' || c == ',';
    }

    private static boolean isClosingBoundary(char c) {
        return c == '.' || c == ',' || c == '!' || c == '?' || c == ':' || c == ';'
                || c == ')' || c == ']' || c == '}' || c == '>';
    }

    private static boolean isApostrophe(String input, int index) {
        return index > 0
                && index + 1 < input.length()
                && Character.isLetterOrDigit(input.charAt(index - 1))
                && Character.isLetterOrDigit(input.charAt(index + 1));
    }

    private static boolean isEscaped(String input, int index) {
        int slashCount = 0;
        for (int i = index - 1; i >= 0 && input.charAt(i) == '\\'; i--) {
            slashCount++;
        }
        return slashCount % 2 == 1;
    }

    private static List<Range> mergeRanges(List<Range> ranges) {
        if (ranges.isEmpty()) {
            return List.of();
        }

        ranges.sort((left, right) -> Integer.compare(left.start(), right.start()));
        List<Range> merged = new ArrayList<>();
        Range current = ranges.get(0);
        for (int i = 1; i < ranges.size(); i++) {
            Range next = ranges.get(i);
            if (next.start() <= current.end()) {
                current = new Range(current.start(), Math.max(current.end(), next.end()));
            } else {
                merged.add(current);
                current = next;
            }
        }
        merged.add(current);
        return merged;
    }

    private record Range(int start, int end) {
    }

    private record QuoteState(int start, char closeQuote) {
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
