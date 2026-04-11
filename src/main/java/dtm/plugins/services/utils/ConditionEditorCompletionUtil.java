package dtm.plugins.services.utils;

import org.fife.ui.autocomplete.AutoCompletion;
import org.fife.ui.autocomplete.BasicCompletion;
import org.fife.ui.autocomplete.DefaultCompletionProvider;
import org.fife.ui.autocomplete.ShorthandCompletion;
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;

import java.util.List;

public final class ConditionEditorCompletionUtil {

    private static final List<String> EXTRA_IMPORTS = List.of(
            "java.util.stream.Collectors",
            "java.util.stream.Stream",
            "java.util.function.Predicate",
            "java.util.function.Function",
            "java.util.function.Consumer",
            "java.util.function.Supplier",
            "java.util.concurrent.TimeUnit",
            "java.util.concurrent.CompletableFuture",
            "java.util.regex.Pattern",
            "java.util.regex.Matcher",
            "java.io.File",
            "java.io.IOException",
            "java.nio.file.Path",
            "java.nio.file.Paths",
            "java.nio.file.Files",
            "java.time.LocalDate",
            "java.time.LocalDateTime",
            "java.time.Instant",
            "java.time.Duration",
            "dtm.stools.component.popup.ModernDialog"
    );

    private ConditionEditorCompletionUtil() {}

    public static AutoCompletion install(RSyntaxTextArea textArea) {
        DefaultCompletionProvider provider = new DefaultCompletionProvider() {
            @Override
            protected boolean isValidChar(char ch) {
                return Character.isLetterOrDigit(ch) || ch == '_' || ch == '.';
            }
        };
        provider.setAutoActivationRules(false, ".");

        addKeywords(provider);
        addEnvironmentApi(provider);
        addUserProcessApi(provider);
        addModernDialogApi(provider);
        addCommonTypes(provider);
        addImportCompletions(provider);

        AutoCompletion ac = new AutoCompletion(provider);
        ac.setAutoActivationEnabled(true);
        ac.setAutoActivationDelay(300);
        ac.setShowDescWindow(true);
        ac.setAutoCompleteSingleChoices(false);
        ac.setParameterAssistanceEnabled(true);
        ac.install(textArea);
        return ac;
    }

    private static void addKeywords(DefaultCompletionProvider p) {
        for (String kw : List.of(
                "abstract", "assert", "boolean", "break", "byte", "case", "catch",
                "char", "class", "continue", "default", "do", "double", "else",
                "enum", "extends", "final", "finally", "float", "for", "if",
                "implements", "import", "instanceof", "int", "interface", "long",
                "new", "null", "package", "private", "protected", "public",
                "return", "short", "static", "super", "switch", "synchronized",
                "this", "throw", "throws", "try", "var", "void", "while",
                "true", "false"
        )) {
            p.addCompletion(new BasicCompletion(p, kw));
        }
    }

    private static void addEnvironmentApi(DefaultCompletionProvider p) {
        String t = "Environment";
        m(p, "env.getProcess()",                  t, "UserProcess getProcess()",                       "Retorna o processo atual");
        m(p, "env.getProcess()",                  t, "UserProcess getProcess(int index)",               "Retorna processo pelo índice");
        m(p, "env.getProcess(\"\")",              t, "UserProcess getProcess(String id)",               "Retorna processo pelo ID");
        m(p, "env.getProcessWithError()",         t, "UserProcess getProcessWithError(int index)",      "Retorna processo com erro pelo índice");
        m(p, "env.getProcessWithError(\"\")",     t, "UserProcess getProcessWithError(String id)",      "Retorna processo com erro pelo ID");
        m(p, "env.getAllProcesses()",              t, "List<UserProcess> getAllProcesses()",              "Retorna todos os processos");
        m(p, "env.getAllWithErrorProcesses()",     t, "List<UserProcess> getAllWithErrorProcesses()",     "Retorna todos os processos com erro");
        m(p, "env.hasProcessById(\"\")",          t, "boolean hasProcessById(String id)",               "Verifica se existe processo com o ID");
        m(p, "env.hasProcessWithErrorById(\"\")", t, "boolean hasProcessWithErrorById(String id)",      "Verifica se existe processo com erro pelo ID");
        m(p, "env.stopSelf()",                    t, "void stopSelf()",                                 "Para o processo atual");
        m(p, "env.getAppDirectory()",             t, "String getAppDirectory()",                        "Retorna o diretório da aplicação");
        m(p, "env.getSystemProperty(\"\")",       t, "String getSystemProperty(String key)",            "Retorna propriedade do sistema");
        m(p, "env.notifyProcess(\"\", )",         t, "void notifyProcess(String id, Object data)",      "Notifica outro processo");
    }

    private static void addUserProcessApi(DefaultCompletionProvider p) {
        String t = "UserProcess";
        m(p, "getProcessDefinition()",    t, "ProcessDefinition getProcessDefinition()", null);
        m(p, "writeInOutputProcess()",    t, "void writeInOutputProcess(Object msg)",    "Escreve na saída padrão");
        m(p, "writeInOutputError()",      t, "void writeInOutputError(Object msg)",      "Escreve na saída de erro");
        m(p, "isMainProcess()",           t, "boolean isMainProcess()",                  null);
        m(p, "getOutput()",              t, "String getOutput()",                       "Retorna a saída do processo");
        m(p, "getExecutionEnvironment()", t, "String getExecutionEnvironment()",         null);
        m(p, "getExitCode()",            t, "int getExitCode()",                        "Retorna o código de saída");
    }

    private static void addModernDialogApi(DefaultCompletionProvider p) {
        m(p, "ModernDialog.builder()",        "ModernDialog",         "ModernDialog.Builder builder()",          "Cria novo builder de diálogo");
        m(p, ".title(\"\")",                  "ModernDialog.Builder", "Builder title(String t)",                 null);
        m(p, ".message(\"\")",                "ModernDialog.Builder", "Builder message(String m)",               null);
        m(p, ".type(ModernDialog.Type.INFO)", "ModernDialog.Builder", "Builder type(Type t)",                    "SUCCESS, ERROR, INFO, QUESTION");
        m(p, ".option(\"\", 0)",              "ModernDialog.Builder", "Builder option(String text, int value)",  null);
        m(p, ".draggable(true)",              "ModernDialog.Builder", "Builder draggable(boolean d)",            null);
        m(p, ".show()",                       "ModernDialog.Builder", "int show()",                              "Exibe o diálogo e retorna o valor");

        for (String v : List.of("SUCCESS", "ERROR", "INFO", "QUESTION")) {
            p.addCompletion(new BasicCompletion(p, "ModernDialog.Type." + v, "ModernDialog.Type", "Tipo: " + v));
        }
    }

    private static void addCommonTypes(DefaultCompletionProvider p) {
        for (String[] e : new String[][]{
                {"String", "java.lang.String"}, {"Integer", "java.lang.Integer"},
                {"Long", "java.lang.Long"}, {"Double", "java.lang.Double"},
                {"Boolean", "java.lang.Boolean"}, {"Math", "java.lang.Math"},
                {"List", "java.util.List"}, {"ArrayList", "java.util.ArrayList"},
                {"Map", "java.util.Map"}, {"HashMap", "java.util.HashMap"},
                {"Set", "java.util.Set"}, {"HashSet", "java.util.HashSet"},
                {"Optional", "java.util.Optional"}, {"Objects", "java.util.Objects"},
                {"Arrays", "java.util.Arrays"}, {"Collections", "java.util.Collections"},
                {"Environment", "dtm.manager.process.interpreter.execution.Environment"},
                {"UserProcess", "dtm.manager.process.interpreter.execution.UserProcess"},
                {"ModernDialog", "dtm.stools.component.popup.ModernDialog"},
        }) p.addCompletion(new ShorthandCompletion(p, e[0], e[0], e[1]));

        m(p, "String.valueOf()",    "String",   "String valueOf(Object o)",             null);
        m(p, "String.format(\"\")", "String",   "String format(String fmt, Object...)", null);
        m(p, "String.join(\"\", )", "String",   "String join(CharSequence delim, ...)", null);
        m(p, "Math.abs()",          "Math",     "int/double abs(n)",                    null);
        m(p, "Math.max(, )",        "Math",     "int/double max(a, b)",                 null);
        m(p, "Math.min(, )",        "Math",     "int/double min(a, b)",                 null);
        m(p, "Math.random()",       "Math",     "double random()",                      "0.0 .. 1.0");
        m(p, "Optional.of()",       "Optional", "Optional<T> of(T value)",              null);
        m(p, "Optional.ofNullable()","Optional","Optional<T> ofNullable(T value)",      null);
        m(p, "Optional.empty()",    "Optional", "Optional<T> empty()",                  null);
        m(p, "List.of()",           "List",     "List<E> of(E... elements)",            "Lista imutável");
        m(p, "Map.of()",            "Map",      "Map<K,V> of(K k, V v, ...)",           "Mapa imutável");
    }

    private static void addImportCompletions(DefaultCompletionProvider p) {
        for (String fqn : EXTRA_IMPORTS) {
            String simple = fqn.substring(fqn.lastIndexOf('.') + 1);
            p.addCompletion(new ShorthandCompletion(p,
                    "import " + simple,
                    "import " + fqn + ";\n",
                    "import",
                    "Importar " + fqn));
        }
    }

    private static void m(DefaultCompletionProvider p, String text, String type, String shortDesc, String summary) {
        BasicCompletion c = new BasicCompletion(p, text, type, summary);
        c.setShortDescription(shortDesc);
        p.addCompletion(c);
    }
}