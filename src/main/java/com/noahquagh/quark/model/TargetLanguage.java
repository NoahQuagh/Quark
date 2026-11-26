package com.noahquagh.quark.model;

/**
 * Langages cibles supportés par le moteur de génération de design patterns.
 *
 * @author noahq
 * @version 1.0
 */
public enum TargetLanguage {
    JAVA(".java"),
    KOTLIN(".kt");

    private final String fileExtension;

    TargetLanguage(String fileExtension) {
        this.fileExtension = fileExtension;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}