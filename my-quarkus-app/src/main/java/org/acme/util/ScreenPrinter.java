package org.acme.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ScreenPrinter {
    public static final int DEFAULT_LINE_WIDTH = 80;

    /**
     * Prints a single line for backwards compatibility.
     * Assumes all configs are on line 1.
     */
    public static String printLine(Object record, List<FieldConfig> configs) {
        return printLine(record, configs, DEFAULT_LINE_WIDTH);
    }

    /**
     * Prints a single line for backwards compatibility.
     * Assumes all configs are on line 1.
     */
    public static String printLine(Object record, List<FieldConfig> configs, int lineWidth) {
        String[] lines = printLines(record, configs, lineWidth);
        if (lines.length == 0) {
            return spaces(lineWidth);
        }
        return lines[0]; // Return first line only
    }

    /**
     * Prints multiple lines for a single record, grouping configs by line number.
     */
    public static String[] printLines(Object record, List<FieldConfig> configs) {
        return printLines(record, configs, DEFAULT_LINE_WIDTH);
    }

    /**
     * Prints multiple lines for a single record, grouping configs by line number.
     */
    public static String[] printLines(Object record, List<FieldConfig> configs, int lineWidth) {
        if (record == null || configs == null || configs.isEmpty()) {
            return new String[]{spaces(lineWidth)};
        }

        // Group configs by line number
        Map<Integer, List<FieldConfig>> configsByLine = configs.stream()
                .filter(cfg -> cfg != null)
                .collect(Collectors.groupingBy(FieldConfig::getLineNumber));

        if (configsByLine.isEmpty()) {
            return new String[]{spaces(lineWidth)};
        }

        // Find the maximum line number
        int maxLine = configsByLine.keySet().stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(1);

        List<String> lines = new ArrayList<>();
        // Process each line from 1 to maxLine
        for (int lineNum = 1; lineNum <= maxLine; lineNum++) {
            List<FieldConfig> lineConfigs = configsByLine.getOrDefault(lineNum, new ArrayList<>());
            char[] buffer = spaces(lineWidth).toCharArray();

            for (FieldConfig config : lineConfigs) {
                int start = Math.max(0, config.getStartIndex());
                if (start >= lineWidth) {
                    continue;
                }
                String value = getStringFieldValue(record, config.getFieldName());
                if (value == null || value.isEmpty()) {
                    continue; // Skip empty/null values
                }
                int maxCopy = Math.min(value.length(), lineWidth - start);
                for (int i = 0; i < maxCopy; i++) {
                    buffer[start + i] = value.charAt(i);
                }
            }
            
            // Only add the line if it has non-space content
            String lineStr = new String(buffer);
            if (!isLineEmpty(lineStr)) {
                lines.add(lineStr);
            }
        }

        // If all lines were empty, return at least one empty line for backwards compatibility
        if (lines.isEmpty()) {
            return new String[]{spaces(lineWidth)};
        }

        return lines.toArray(new String[0]);
    }

    public static String[] print(Collection<?> records, List<FieldConfig> configs) {
        return print(records, configs, DEFAULT_LINE_WIDTH);
    }

    public static String[] print(Collection<?> records, List<FieldConfig> configs, int lineWidth) {
        if (records == null) {
            return new String[0];
        }
        List<String> allLines = new ArrayList<>();
        for (Object record : records) {
            String[] recordLines = printLines(record, configs, lineWidth);
            for (String line : recordLines) {
                allLines.add(line);
            }
        }
        return allLines.toArray(new String[0]);
    }

    private static String getStringFieldValue(Object record, String fieldName) {
        if (record == null || fieldName == null || fieldName.isEmpty()) {
            return null;
        }
        Class<?> clazz = record.getClass();
        Field field = findField(clazz, fieldName);
        if (field == null) {
            return null;
        }
        boolean accessible = field.canAccess(record);
        try {
            if (!accessible) {
                field.setAccessible(true);
            }
            Object value = field.get(record);
            if (value == null) {
                return null;
            }
            if (value instanceof String) {
                return (String) value;
            }
            return String.valueOf(value);
        } catch (IllegalAccessException ignored) {
            return null;
        } finally {
            if (!accessible) {
                try {
                    field.setAccessible(false);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Field findField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private static String spaces(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /**
     * Checks if a line is empty (contains only whitespace characters).
     */
    private static boolean isLineEmpty(String line) {
        if (line == null || line.isEmpty()) {
            return true;
        }
        for (int i = 0; i < line.length(); i++) {
            if (!Character.isWhitespace(line.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
