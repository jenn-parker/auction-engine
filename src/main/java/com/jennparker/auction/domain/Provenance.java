package com.jennparker.auction.domain;

public final class Provenance {
    private final String summary, previousOwner, documentationReference;
    public Provenance(String summary, String previousOwner, String documentationReference) {
        this.summary = summary; this.previousOwner = previousOwner; this.documentationReference = documentationReference;
    }
    public String getSummary() { return summary; } public String getPreviousOwner() { return previousOwner; }
    public String getDocumentationReference() { return documentationReference; }
}
