package com.dabomstew.pkrandom.romhandlers.romentries;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.function.BiConsumer;

import static com.dabomstew.pkrandom.FileFunctions.openConfig;

public abstract class BaseRomEntryReader<T extends RomEntry> {

    public static int parseInt(String s) {
        int radix = 10;
        s = s.trim().toLowerCase();
        if (s.startsWith("0x") || s.startsWith("&h")) {
            radix = 16;
            s = s.substring(2);
        }
        try {
            return Integer.parseInt(s, radix);
        } catch (NumberFormatException ex) {
            System.err.println("invalid base " + radix + " number " + s);
            return 0;
        }
    }

    public static long parseLong(String s) {
        int radix = 10;
        s = s.trim().toLowerCase();
        if (s.startsWith("0x") || s.startsWith("&h")) {
            radix = 16;
            s = s.substring(2);
        }
        try {
            return Long.parseLong(s, radix);
        } catch (NumberFormatException ex) {
            System.err.println("invalid base " + radix + " number " + s);
            return 0;
        }
    }

    public static boolean parseBoolean(String s) {
        return (parseInt(s) > 0);
    }

    /**
     * An enum for dictating whether to read values as ints or Strings by default.
     * Protected so subclasses have to make the choice, instead of the code using them.
     */
    protected enum DefaultReadMode { INT, STRING }

    protected enum CopyFromMode { NAME, ROMCODE }

    private static final String COMMENT_PREFIX = "//";

    private final Scanner scanner;
    private final DefaultReadMode defaultReadMode;
    private final CopyFromMode copyFromMode;
    private T current;
    private Collection<T> romEntries;

    private final Map<String, BiConsumer<T, String[]>> keyPrefixMethods = new HashMap<>();
    private final Map<String, BiConsumer<T, String[]>> keySuffixMethods = new HashMap<>();
    private final Map<String, BiConsumer<T, String>> specialKeyMethods = new HashMap<>();

    public BaseRomEntryReader(String fileName, DefaultReadMode defaultReadMode,
                              CopyFromMode copyFromMode) throws IOException {
        this.scanner = new Scanner(openConfig(fileName), StandardCharsets.UTF_8);
        this.defaultReadMode = defaultReadMode;
        this.copyFromMode = copyFromMode;
        putSpecialKeyMethod("CopyFrom", this::copyFrom);
    }

    protected void putKeyPrefixMethod(String key, BiConsumer<T, String[]> method) {
        keyPrefixMethods.put(key, method);
    }

    protected void putKeySuffixMethod(String key, BiConsumer<T, String[]> method) {
        keySuffixMethods.put(key, method);
    }

    protected void putSpecialKeyMethod(String key, BiConsumer<T, String> method) {
        specialKeyMethods.put(key, method);
    }

    public void readAllRomEntries(Collection<T> romEntries) {
        this.romEntries = romEntries;

        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            line = removeComments(line);

            if (line.isEmpty()) {
                continue;
            }

            if (isRomEntryStart(line)) {
                startNewRomEntry(line);
            } else if (current != null) {
                parseAndAddValuePair(line);
            }

        }
    }

    private boolean isRomEntryStart(String line) {
        return line.startsWith("[") && line.endsWith("]");
    }

    private void startNewRomEntry(String line) {
        current = initiateRomEntry(line.substring(1, line.length() - 1));
        romEntries.add(current);
    }

    protected abstract T initiateRomEntry(String name);

    private String removeComments(String line) {
        if (line.contains(COMMENT_PREFIX)) {
            line = line.substring(0, line.indexOf(COMMENT_PREFIX)).trim();
        }
        return line;
    }

    private void parseAndAddValuePair(String line) {
        String[] valuePair = splitIntoValuePair(line);
        if (valuePair == null) {
            throw new RuntimeException(); // TODO: what exactly to throw here
        }

        BiConsumer<T, String[]> keyPrefixMethod = checkForKeyPrefixMethod(valuePair);
        if (keyPrefixMethod != null) {
            keyPrefixMethod.accept(current, valuePair);
            return;
        }

        BiConsumer<T, String[]> keySuffixMethod = checkForKeySuffixMethod(valuePair);
        if (keySuffixMethod != null) {
            keySuffixMethod.accept(current, valuePair);
            return;
        }

        BiConsumer<T, String> specialKeyMethod = specialKeyMethods.get(valuePair[0]);
        if (specialKeyMethod != null) {
            specialKeyMethod.accept(current, valuePair[1]);
            return;
        }

        addNormalValue(valuePair);
    }

    private BiConsumer<T, String[]> checkForKeyPrefixMethod(String[] valuePair) {
        for (Map.Entry<String, BiConsumer<T, String[]>> ksmEntry : keyPrefixMethods.entrySet()) {
            if (valuePair[0].startsWith(ksmEntry.getKey())) {
                return ksmEntry.getValue();
            }
        }
        return null;
    }

    private BiConsumer<T, String[]> checkForKeySuffixMethod(String[] valuePair) {
        for (Map.Entry<String, BiConsumer<T, String[]>> kfmEntry : keySuffixMethods.entrySet()) {
            if (valuePair[0].endsWith(kfmEntry.getKey())) {
                return kfmEntry.getValue();
            }
        }
        return null;
    }

    private String[] splitIntoValuePair(String line) {
        String[] valuePair = line.split("=", 2);
        if (valuePair.length == 1) {
            System.err.println("invalid entry " + line);
            return null;
        }
        if (valuePair[1].endsWith("\r\n")) {
            valuePair[1] = valuePair[1].substring(0, valuePair[1].length() - 2);
        }
        valuePair[1] = valuePair[1].trim();
        valuePair[0] = valuePair[0].trim();
        return valuePair;
    }

    private void addNormalValue(String[] valuePair) {
        if (isArrayValuePair(valuePair)) {
            addArrayValue(valuePair);
        } else {
            switch (defaultReadMode) {
                case INT -> addIntValue(valuePair);
                case STRING -> addStringValue(valuePair);
            }
        }
    }

    private boolean isArrayValuePair(String[] valuePair) {
        return valuePair[1].startsWith("[") && valuePair[1].endsWith("]");
    }

    protected void addArrayValue(String[] valuePair) {
        String[] unparsed = valuePair[1].substring(1, valuePair[1].length() - 1).split(",");
        if (unparsed.length == 1 && unparsed[0].trim().isEmpty()) {
            current.putArrayValue(valuePair[0], new int[0]);
        } else {
            int[] value = new int[unparsed.length];
            int i = 0;
            for (String s : unparsed) {
                value[i++] = parseInt(s);
            }
            current.putArrayValue(valuePair[0], value);
        }
    }

    private void addIntValue(String[] valuePair) {
        int value = parseInt(valuePair[1]);
        current.putIntValue(valuePair[0], value);
    }

    // TODO: kind of redundant with private method in RomEntry class
    private void addStringValue(String[] valuePair) {
        current.putStringValue(valuePair[0], valuePair[1]);
    }

    private void copyFrom(T romEntry, String value) {
        for (T other : romEntries) {
            if (matchesCopyFromValue(other, value)) {
                romEntry.copyFrom(other);
            }
        }
    }

    private boolean matchesCopyFromValue(T other, String value) {
        return switch (copyFromMode) {
            case NAME -> value.equalsIgnoreCase(other.getName());
            case ROMCODE -> value.equals(other.getRomCode());
        };
    }

}
