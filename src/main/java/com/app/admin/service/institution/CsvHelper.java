package com.app.admin.service.institution;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.app.admin.dto.SourceRow;

public final class CsvHelper {

    private CsvHelper() {}

    public static List<SourceRow> parseCsv(InputStream inputStream) throws Exception {
        List<SourceRow> rows = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        String line = reader.readLine();
        if (line == null) {
            return rows;
        }

        // Strip UTF-8 BOM if present
        if (line.startsWith("\uFEFF")) {
            line = line.substring(1);
        }

        List<String> headers = parseCsvLine(line);
        if (headers.isEmpty()) {
            return rows;
        }

        // Clean headers: lowercase/trimmed for key mapping, but keep exact header matching
        List<String> cleanHeaders = new ArrayList<>();
        for (String h : headers) {
            cleanHeaders.add(h != null ? h.trim() : "");
        }

        StringBuilder multiLineBuffer = new StringBuilder();
        boolean insideQuotes = false;
        int rowIndex = 1;

        while ((line = reader.readLine()) != null) {
            if (insideQuotes) {
                multiLineBuffer.append("\n").append(line);
            } else {
                multiLineBuffer.setLength(0);
                multiLineBuffer.append(line);
            }

            // Check if quotes are balanced
            insideQuotes = hasUnbalancedQuotes(multiLineBuffer.toString());
            if (insideQuotes) {
                continue;
            }

            String fullLine = multiLineBuffer.toString().trim();
            if (fullLine.isEmpty()) {
                continue;
            }

            List<String> values = parseCsvLine(fullLine);
            Map<String, String> rowMap = new LinkedHashMap<>();

            for (int i = 0; i < cleanHeaders.size(); i++) {
                String header = cleanHeaders.get(i);
                if (header.isEmpty()) continue;
                String val = (i < values.size()) ? values.get(i) : "";
                rowMap.put(header, val != null ? val.trim() : "");
            }

            rows.add(new SourceRow(rowIndex++, rowMap));
        }

        return rows;
    }

    private static boolean hasUnbalancedQuotes(String text) {
        boolean inQuote = false;
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == '"') {
                if (i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    i++; // Skip escaped quote
                } else {
                    inQuote = !inQuote;
                }
            }
        }
        return inQuote;
    }

    public static List<String> parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        if (line == null) {
            return values;
        }

        StringBuilder sb = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    sb.append('"');
                    i++; // skip escaped quote
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                values.add(sb.toString());
                sb.setLength(0);
            } else {
                sb.append(c);
            }
        }
        values.add(sb.toString());

        return values;
    }
}
