package com.zebra.aisuite_quickstart.filtertracker;

public enum FilterType {
    BARCODE, // Represents only the Barcode checkbox being selected
    OCR,     // Represents only the OCR checkbox being selected
    BOTH,    // Represents both checkboxes being selected
    NONE;    // Represents no checkbox being selected

    // Helper method to determine the filter type based on the checkbox states
    public static FilterType getFilterType(boolean isBarcodeChecked, boolean isOcrChecked) {
        if (isBarcodeChecked && isOcrChecked) {
            return BOTH;
        } else if (isBarcodeChecked) {
            return BARCODE;
        } else if (isOcrChecked) {
            return OCR;
        } else {
            return NONE;
        }
    }
}
