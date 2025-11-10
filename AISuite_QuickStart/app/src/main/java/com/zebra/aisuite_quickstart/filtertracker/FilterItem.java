package com.zebra.aisuite_quickstart.filtertracker;

public class FilterItem {
    private String title;
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
