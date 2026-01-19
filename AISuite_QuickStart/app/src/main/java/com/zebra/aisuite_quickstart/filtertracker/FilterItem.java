// Copyright 2025 Zebra Technologies Corporation and/or its affiliates. All rights reserved.
package com.zebra.aisuite_quickstart.filtertracker;

public class FilterItem {
    private final String title;
    private boolean isChecked;

    public FilterItem(String title) {
        this.title = title;
        this.isChecked = true;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public String getTitle() {
        return title;
    }

    public boolean isChecked() {
        return isChecked;
    }

}
