package dtm.plugins.services.utils;

public class LogColorizer {

    private static final String TS = "^((?:\\d{2,4}[/\\-]\\d{2}[/\\-]\\d{2,4}[T ])?\\d{2}:\\d{2}[:\\d.+\\-]*)\\s+";

    private static final String RST  = "\033[0m";
    private static final String RED  = "\033[31m";
    private static final String GRN  = "\033[32m";
    private static final String YEL  = "\033[33m";
    private static final String BLU  = "\033[34m";
    private static final String MAG  = "\033[35m";
    private static final String CYA  = "\033[36m";
    private static final String WHT  = "\033[37m";
    private static final String GRY  = "\033[90m";
    private static final String BRED = "\033[91m";
    private static final String BGRN = "\033[92m";
    private static final String BYEL = "\033[93m";
    private static final String BBLU = "\033[94m";
    private static final String BMAG = "\033[95m";
    private static final String BCYA = "\033[96m";

    private LogColorizer() {}

    public static String colorize(String text) {
        if (isBannerOrAsciiArt(text)) {
            return text;
        }
        if (isDebugOrTrace(text)) {
            return GRY + text + RST;
        }
        text = colorizeBareLevel(text);
        text = colorizeTimestamps(text);
        text = colorizeLevelBrackets(text);
        text = colorizeUrls(text);
        text = colorizeExceptions(text);
        text = colorizeThreads(text);
        text = colorizePid(text);
        text = colorizeStrings(text);
        text = colorizeDurations(text);
        text = colorizeSizes(text);
        text = colorizeIpsAndPorts(text);
        text = colorizeUuids(text);
        text = colorizeVersions(text);
        text = colorizeClassNames(text);
        text = colorizeBooleans(text);
        text = colorizeStatusKeywords(text);
        return text;
    }

    private static boolean isBannerOrAsciiArt(String text) {
        String trimmed = text.trim();
        if (trimmed.isEmpty()) return true;
        int special = 0;
        int total = trimmed.length();
        for (int i = 0; i < total; i++) {
            char c = trimmed.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != ' ') special++;
        }
        return (special > total * 0.4) || trimmed.startsWith("::") || trimmed.startsWith("=====");
    }

    private static boolean isDebugOrTrace(String text) {
        return text.contains("[DEBUG]") || text.contains("[TRACE]") ||
                text.contains("[DBG]")   || text.contains("[TRC]")   ||
                text.contains("[VERBOSE]")|| text.contains("[FINE]") ||
                text.matches(TS + "(DEBUG|TRACE|DBG|TRC|VERBOSE|FINE)\\b.*");
    }

    private static String colorizeTimestamps(String text) {
        return text.replaceAll(
                "(\\d{4}-\\d{2}-\\d{2}[T ])?((\\d{2}:\\d{2}:\\d{2})(?:\\.\\d+)?)",
                GRY + "$0" + RST);
    }

    private static String colorizeLevelBrackets(String text) {
        return text
                .replace("[ERROR]",    "[" + RED + "ERROR"    + RST + "]")
                .replace("[ERRO]",     "[" + RED + "ERRO"     + RST + "]")
                .replace("[SEVERE]",   "[" + RED + "SEVERE"   + RST + "]")
                .replace("[ERR]",      "[" + RED + "ERR"      + RST + "]")
                .replace("[FATAL]",    "[" + BRED + "FATAL"   + RST + "]")
                .replace("[CRITICAL]", "[" + BRED + "CRITICAL"+ RST + "]")
                .replace("[CRIT]",     "[" + BRED + "CRIT"    + RST + "]")
                .replace("[FAIL]",     "[" + RED + "FAIL"     + RST + "]")
                .replace("[FAILED]",   "[" + RED + "FAILED"   + RST + "]")
                .replace("[WARN]",     "[" + YEL + "WARN"     + RST + "]")
                .replace("[WARNING]",  "[" + YEL + "WARNING"  + RST + "]")
                .replace("[WRN]",      "[" + YEL + "WRN"      + RST + "]")
                .replace("[INFO]",     "[" + BGRN + "INFO"    + RST + "]")
                .replace("[INF]",      "[" + BGRN + "INF"     + RST + "]")
                .replace("[NOTICE]",   "[" + CYA + "NOTICE"   + RST + "]");
    }

    private static String colorizeBareLevel(String text) {
        text = text.replaceAll(TS + "(ERROR|ERRO|SEVERE|ERR|FAIL|FAILED)\\b",
                "$1 " + RED + "$2" + RST);
        text = text.replaceAll(TS + "(FATAL|CRITICAL|CRIT)\\b",
                "$1 " + BRED + "$2" + RST);
        text = text.replaceAll(TS + "(WARN|WARNING|WRN)\\b",
                "$1 " + YEL + "$2" + RST);
        text = text.replaceAll(TS + "(INFO|INF|NOTICE)\\b",
                "$1 " + BGRN + "$2" + RST);

        return text;
    }

    private static String colorizeUrls(String text) {
        return text.replaceAll(
                "(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
                BLU + "$1" + RST);
    }

    private static String colorizeExceptions(String text) {
        text = text.replaceAll("(Caused by:)", BRED + "$1" + RST);
        text = text.replaceAll("([\\w.]+(?:Exception|Error)\\b)", RED + "$1" + RST);
        text = text.replaceAll("(\\s+at [\\w.$]+)\\((.*?)\\)",
                GRY + "$1" + RST + "(" + GRY + "$2" + RST + ")");
        return text;
    }

    private static String colorizeThreads(String text) {
        return text.replaceAll(
                "(\\[(?:pool-[\\d-]+thread-\\d+|main|\\w+-\\d+)])(?!\\])",
                MAG + "$1" + RST);
    }

    private static String colorizePid(String text) {
        return text.replaceAll("\\b(PID\\s+\\d+)", BMAG + "$1" + RST);
    }

    private static String colorizeStrings(String text) {
        text = text.replaceAll("(\"[^\"]+\")", YEL + "$1" + RST);
        text = text.replaceAll("('[^']+')", BYEL + "$1" + RST);
        return text;
    }

    private static String colorizeDurations(String text) {
        return text.replaceAll("(\\d+(?:\\.\\d+)?\\s*(?:ms|ns|µs|s)\\b)", BYEL + "$1" + RST);
    }

    private static String colorizeSizes(String text) {
        return text.replaceAll("(\\d+(?:\\.\\d+)?\\s*(?:MB|KB|GB|TB|bytes?)\\b)", BCYA + "$1" + RST);
    }

    private static String colorizeIpsAndPorts(String text) {
        text = text.replaceAll(
                "(?<!\\d)(\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(?!\\d)",
                MAG + "$1" + RST);
        text = text.replaceAll(
                "(?<=\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3})(:\\d{2,5})\\b",
                BMAG + "$1" + RST);
        text = text.replaceAll("(?i)(port\\s+)(\\d{2,5})\\b",
                "$1" + BMAG + "$2" + RST);
        return text;
    }

    private static String colorizeUuids(String text) {
        return text.replaceAll(
                "([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})",
                BBLU + "$1" + RST);
    }

    private static String colorizeVersions(String text) {
        text = text.replaceAll("(?<=\\s|^)(v\\d+\\.\\d+[.\\w]*)", CYA + "$1" + RST);
        text = text.replaceAll("(?<=version\\s)(\\d+\\.\\d+[.\\w]*)", CYA + "$1" + RST);
        return text;
    }

    private static String colorizeClassNames(String text) {
        text = text.replaceAll(
                "(?<=\\s)((?:[a-z_][a-z0-9_]*\\.){2,}[A-Za-z_][A-Za-z0-9_$]*)(?=\\s|:|$)",
                WHT + "$1" + RST);
        text = text.replaceAll(
                "(?<=\\s)([A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+){1,})(?=\\s|:|\\.|$)",
                WHT + "$1" + RST);
        return text;
    }

    private static String colorizeBooleans(String text) {
        return text.replaceAll("\\b(true|false|null|nil|None|TRUE|FALSE|NULL)\\b",
                BMAG + "$1" + RST);
    }

    private static String colorizeStatusKeywords(String text) {
        return text.replaceAll(
                "(?i)\\b(enabled|disabled|started|stopped|failed|success|completed|initialized|registered|loaded|connecting|connected|disconnected|shutting down|shutdown)\\b",
                BGRN + "$1" + RST);
    }
}