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

    public String displayName(){
        String[] words = name().toLowerCase().split("_");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(' ');
            }
        }
        return result.toString().trim();
    }

    @Override
    public String toString() {
        return displayName();
    }
}