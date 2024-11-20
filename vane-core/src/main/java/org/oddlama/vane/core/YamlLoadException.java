package org.oddlama.vane.core;

import org.oddlama.vane.core.lang.LangField;

@SuppressWarnings("serial")
public class YamlLoadException extends Exception {

    public YamlLoadException(String message) {
        super(message);
    }

    public YamlLoadException() {
        super();
    }

    public YamlLoadException(String message, Throwable cause) {
        super(message, cause);
    }

    public YamlLoadException(Throwable cause) {
        super(cause);
    }

    protected YamlLoadException(
        String message,
        Throwable cause,
        boolean enableSuppression,
        boolean writableStackTrace
    ) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    public static class Lang extends YamlLoadException {

        public final LangField<?> langField;

        @Override
        public String getMessage() {
            return "[" + this.langField.toString() + "] " + super.getMessage();
        }

        public <T> Lang(String message, LangField<?> erroredField) {
            super(message);
            this.langField = erroredField;
        }
    }
}
