import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class CsvUtils {
    private static final Charset FALLBACK_CHARSET = Charset.forName("windows-1252");

    private CsvUtils() {
    }

    public static List<Map<String, String>> readRows(String fileName) {
        try (BufferedReader reader = new BufferedReader(openReader(fileName))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return List.of();
            }

            List<String> headers = parseLine(stripBom(headerLine));
            List<Map<String, String>> rows = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) {
                    continue;
                }

                List<String> values = parseLine(line);
                Map<String, String> row = new LinkedHashMap<>();
                for (int index = 0; index < headers.size(); index++) {
                    String value = index < values.size() ? values.get(index) : "";
                    row.put(headers.get(index), value);
                }
                rows.add(row);
            }
            return rows;
        } catch (IOException exception) {
            throw new UncheckedIOException("Unable to read CSV file: " + fileName, exception);
        }
    }

    private static Reader openReader(String fileName) throws IOException {
        Path path = resolvePath(fileName);
        if (path != null) {
            byte[] bytes = Files.readAllBytes(path);
            return new InputStreamReader(new ByteArrayInputStream(bytes), detectCharset(bytes));
        }

        byte[] resourceBytes = readClasspathResource(fileName);
        if (resourceBytes == null) {
            throw new IOException("File not found: " + fileName);
        }
        return new InputStreamReader(new ByteArrayInputStream(resourceBytes), detectCharset(resourceBytes));
    }

    private static byte[] readClasspathResource(String fileName) throws IOException {
        ClassLoader classLoader = CsvUtils.class.getClassLoader();
        for (String candidate : resourceCandidates(fileName)) {
            try (InputStream inputStream = classLoader.getResourceAsStream(candidate)) {
                if (inputStream != null) {
                    return inputStream.readAllBytes();
                }
            }
        }
        return null;
    }

    private static Path resolvePath(String fileName) {
        for (String candidate : pathCandidates(fileName)) {
            Path path = Paths.get(candidate);
            if (Files.exists(path)) {
                return path;
            }
        }
        return null;
    }

    private static List<String> pathCandidates(String fileName) {
        String normalized = normalize(fileName);
        String baseName = Paths.get(normalized).getFileName().toString();
        return List.of(
                normalized,
                "src/main/resources/" + normalized,
                "src/main/resources/data/" + baseName,
                "data/" + baseName
        );
    }

    private static List<String> resourceCandidates(String fileName) {
        String normalized = normalize(fileName);
        String baseName = Paths.get(normalized).getFileName().toString();
        return List.of(
                normalized,
                "data/" + baseName,
                baseName
        );
    }

    private static Charset detectCharset(byte[] bytes) {
        try {
            StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(bytes));
            return StandardCharsets.UTF_8;
        } catch (CharacterCodingException exception) {
            return FALLBACK_CHARSET;
        }
    }

    private static List<String> parseLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder currentValue = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (inQuotes && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    currentValue.append('"');
                    index++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (character == ',' && !inQuotes) {
                values.add(currentValue.toString().trim());
                currentValue.setLength(0);
            } else {
                currentValue.append(character);
            }
        }

        values.add(currentValue.toString().trim());
        return values;
    }

    private static String normalize(String fileName) {
        return fileName.replace('\\', '/').replaceFirst("^\\./", "");
    }

    private static String stripBom(String value) {
        if (!value.isEmpty() && value.charAt(0) == '\ufeff') {
            return value.substring(1);
        }
        return value;
    }
}
