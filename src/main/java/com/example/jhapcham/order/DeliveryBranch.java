package com.example.jhapcham.order;

public enum DeliveryBranch {

    // Define the specific fulfillment branches
    KATHMANDU,
    UDAYAPUR,
    MUSTANG;

    /**
     * Converts a raw string value (from request body/param) into the DeliveryBranch enum instance.
     * This method handles case insensitivity and whitespace.
     * * @param value The raw string value of the branch name.
     * @return The corresponding DeliveryBranch enum.
     * @throws IllegalArgumentException if the string does not match any enum constant.
     */
    public static DeliveryBranch fromString(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("Branch name cannot be null or empty.");
        }
        return DeliveryBranch.valueOf(value.trim().toUpperCase());
    }
}