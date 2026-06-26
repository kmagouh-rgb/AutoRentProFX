package com.autorentpro.model;

public class SearchResult {
    private final String type;
    private final String title;
    private final String subtitle;
    private final String status;

    public SearchResult(String type, String title, String subtitle, String status) {
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.status = status;
    }

    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getSubtitle() { return subtitle; }
    public String getStatus() { return status; }
}
