package com.betterprompt.betterpromptbyandyy2.optimizer.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Applies text transformations only outside Markdown code spans.
 */
public final class ProtectedTextProcessor {

    private ProtectedTextProcessor() {
    }

    public static String transformOutsideMarkdownCode(String text, UnaryOperator<String> transformer) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        List<Segment> segments = splitIntoSegments(text);
        StringBuilder result = new StringBuilder(text.length());
        for (Segment segment : segments) {
            if (segment.isProtectedSegment()) {
                result.append(segment.getText());
            } else {
                result.append(transformer.apply(segment.getText()));
            }
        }
        return result.toString();
    }

    public static List<Segment> splitIntoSegments(String input) {
        if (input == null || input.isEmpty()) {
            return List.of(new Segment(input == null ? "" : input, false));
        }

        List<Range> protectedRanges = findProtectedRanges(input);
        if (protectedRanges.isEmpty()) {
            return List.of(new Segment(input, false));
        }

        List<Segment> segments = new ArrayList<>();
        int cursor = 0;
        for (Range range : protectedRanges) {
            if (cursor < range.start) {
                segments.add(new Segment(input.substring(cursor, range.start), false));
            }
            segments.add(new Segment(input.substring(range.start, range.end), true));
            cursor = range.end;
        }
        if (cursor < input.length()) {
            segments.add(new Segment(input.substring(cursor), false));
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

    private static List<Range> findProtectedRanges(String text) {
        List<Range> ranges = findFencedCodeBlocks(text);

        int cursor = 0;
        for (Range fencedRange : new ArrayList<>(ranges)) {
            findInlineCodeSpans(text, cursor, fencedRange.start, ranges);
            cursor = fencedRange.end;
        }
        findInlineCodeSpans(text, cursor, text.length(), ranges);

        ranges.sort(Comparator.comparingInt(range -> range.start));
        return ranges;
    }

    private static List<Range> findFencedCodeBlocks(String text) {
        List<Range> ranges = new ArrayList<>();
        int lineStart = 0;

        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(text, lineStart);
            Fence openingFence = parseFence(text, lineStart, lineEnd);

            if (openingFence == null) {
                lineStart = nextLineStart(text, lineEnd);
                continue;
            }

            int blockEnd = text.length();
            int searchLineStart = nextLineStart(text, lineEnd);
            while (searchLineStart < text.length()) {
                int searchLineEnd = findLineEnd(text, searchLineStart);
                Fence closingFence = parseFence(text, searchLineStart, searchLineEnd);
                if (closingFence != null
                        && closingFence.marker == openingFence.marker
                        && closingFence.length >= openingFence.length) {
                    blockEnd = nextLineStart(text, searchLineEnd);
                    break;
                }
                searchLineStart = nextLineStart(text, searchLineEnd);
            }

            ranges.add(new Range(lineStart, blockEnd));
            lineStart = blockEnd;
        }

        return ranges;
    }

    private static void findInlineCodeSpans(String text, int start, int end, List<Range> ranges) {
        int index = start;
        while (index < end) {
            if (text.charAt(index) != '`') {
                index++;
                continue;
            }

            int tickEnd = index + 1;
            while (tickEnd < end && text.charAt(tickEnd) == '`') {
                tickEnd++;
            }

            int closing = findClosingBackticks(text, tickEnd, end, tickEnd - index);
            if (closing < 0) {
                index = tickEnd;
                continue;
            }

            ranges.add(new Range(index, closing + (tickEnd - index)));
            index = closing + (tickEnd - index);
        }
    }

    private static int findClosingBackticks(String text, int start, int end, int length) {
        for (int index = start; index <= end - length; index++) {
            if (text.charAt(index) != '`') {
                continue;
            }

            boolean matches = true;
            for (int offset = 1; offset < length; offset++) {
                if (text.charAt(index + offset) != '`') {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return index;
            }
        }
        return -1;
    }

    private static Fence parseFence(String text, int lineStart, int lineEnd) {
        int index = lineStart;
        int indent = 0;
        while (index < lineEnd && text.charAt(index) == ' ' && indent < 4) {
            index++;
            indent++;
        }
        if (indent > 3 || index >= lineEnd) {
            return null;
        }

        char marker = text.charAt(index);
        if (marker != '`' && marker != '~') {
            return null;
        }

        int markerEnd = index + 1;
        while (markerEnd < lineEnd && text.charAt(markerEnd) == marker) {
            markerEnd++;
        }

        int length = markerEnd - index;
        if (length < 3) {
            return null;
        }
        return new Fence(marker, length);
    }

    private static int findLineEnd(String text, int lineStart) {
        int newline = text.indexOf('\n', lineStart);
        if (newline < 0) {
            return text.length();
        }
        return newline;
    }

    private static int nextLineStart(String text, int lineEnd) {
        if (lineEnd >= text.length()) {
            return text.length();
        }
        return lineEnd + 1;
    }

    private record Range(int start, int end) {
    }

    private record Fence(char marker, int length) {
    }
}
