package com.agentic.service;

import org.springframework.stereotype.Service;
import java.util.*;

/**
 * Merchant Category Service
 * 
 * Maps merchant names/descriptions to standardized spending categories.
 * Uses pattern matching, aliases, and ML-ready categorization rules.
 * 
 * PHASE 5: Merchant Categorization
 */
@Service
public class MerchantCategoryService {

    /**
     * Standard spending categories for financial analysis
     */
    public enum SpendingCategory {
        GROCERIES("Groceries & Food", "Food"),
        RESTAURANTS("Restaurants & Dining", "Food"),
        TRANSPORT("Transport & Travel", "Travel"),
        UTILITIES("Utilities & Bills", "Bills"),
        ENTERTAINMENT("Entertainment", "Entertainment"),
        SHOPPING("Shopping & Retail", "Shopping"),
        HEALTHCARE("Healthcare & Medical", "Health"),
        INSURANCE("Insurance", "Insurance"),
        EDUCATION("Education & Learning", "Education"),
        SUBSCRIPTIONS("Subscriptions & Memberships", "Subscriptions"),
        PERSONAL_CARE("Personal Care & Beauty", "Personal"),
        TELECOMMUNICATIONS("Telecommunications", "Bills"),
        FINANCIAL_SERVICES("Financial Services & Fees", "Finance"),
        GIFTS("Gifts & Donations", "Social"),
        UNCATEGORIZED("Other", "Other");

        private final String displayName;
        private final String shortName;

        SpendingCategory(String displayName, String shortName) {
            this.displayName = displayName;
            this.shortName = shortName;
        }

        public String getDisplayName() { return displayName; }
        public String getShortName() { return shortName; }
    }

    // Merchant keyword patterns for categorization
    private static final Map<SpendingCategory, List<String>> MERCHANT_PATTERNS = new HashMap<>();

    static {
        MERCHANT_PATTERNS.put(SpendingCategory.GROCERIES, Arrays.asList(
            "super", "market", "grocery", "whole foods", "tesco", "sainsbury", "asda", "morrisons",
            "coop", "sainsburys", "carrefour", "auchan", "coles", "woolworths", "fresh"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.RESTAURANTS, Arrays.asList(
            "cafe", "coffee", "restaurant", "pizza", "burger", "mcdonalds", "kfc", "dominos",
            "subway", "starbucks", "dunkin", "chipotle", "taco", "noodle", "diner", "bar",
            "pub", "bistro", "grill", "steakhouse", "dining"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.TRANSPORT, Arrays.asList(
            "uber", "lyft", "taxi", "transport", "airline", "aviation", "hotel", "motel",
            "airbnb", "booking", "expedia", "flight", "ticket", "train", "railway", "bus",
            "shuttle", "parking", "gas", "fuel", "petrol", "diesel"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.UTILITIES, Arrays.asList(
            "electric", "water", "gas", "utility", "power", "energy", "broadband", "internet",
            "isp", "phone bill", "mobile", "telecom", "veolia", "sewerage"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.ENTERTAINMENT, Arrays.asList(
            "cinema", "movie", "theatre", "theater", "concert", "spotify", "netflix", "hulu",
            "disney", "game", "steam", "playstation", "xbox", "gopro", "sony", "entertainment"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.SHOPPING, Arrays.asList(
            "amazon", "ebay", "shop", "store", "mall", "retail", "clothing", "fashion",
            "boots", "next", "marks & spencer", "h&m", "zara", "primark", "uniqlo",
            "john lewis", "debenhams", "habitat", "ikea", "furniture", "home"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.HEALTHCARE, Arrays.asList(
            "doctor", "medical", "hospital", "clinic", "pharmacy", "chemist", "health",
            "dental", "dentist", "optician", "nhs", "private", "therapy", "physio"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.INSURANCE, Arrays.asList(
            "insurance", "axa", "zurich", "allianz", "aviva", "direct", "cover", "premium"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.EDUCATION, Arrays.asList(
            "school", "university", "course", "training", "education", "tuition", "online",
            "coursera", "udemy", "skillshare", "academy", "lesson"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.SUBSCRIPTIONS, Arrays.asList(
            "subscription", "recurring", "membership", "gym", "fitness", "magazine",
            "journal", "library", "club"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.PERSONAL_CARE, Arrays.asList(
            "beauty", "cosmetics", "salon", "barber", "hair", "spa", "wellness", "skincare",
            "fragrance", "perfume", "makeup"
        ));

        MERCHANT_PATTERNS.put(SpendingCategory.GIFTS, Arrays.asList(
            "gift", "flowers", "charity", "donation", "nonprofit", "ngo", "welfare"
        ));
    }

    /**
     * Map merchant name to spending category using pattern matching
     * @param merchantName Merchant or retailer name
     * @return Identified spending category (or UNCATEGORIZED if not found)
     */
    public SpendingCategory categorizeMerchant(String merchantName) {
        if (merchantName == null || merchantName.isEmpty()) {
            return SpendingCategory.UNCATEGORIZED;
        }

        String lowerMerchant = merchantName.toLowerCase().trim();

        // Exact match first (faster)
        for (Map.Entry<SpendingCategory, List<String>> entry : MERCHANT_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (lowerMerchant.equals(pattern)) {
                    return entry.getKey();
                }
            }
        }

        // Fuzzy match (contains)
        for (Map.Entry<SpendingCategory, List<String>> entry : MERCHANT_PATTERNS.entrySet()) {
            for (String pattern : entry.getValue()) {
                if (lowerMerchant.contains(pattern)) {
                    return entry.getKey();
                }
            }
        }

        return SpendingCategory.UNCATEGORIZED;
    }

    /**
     * Get category display name
     */
    public String getCategoryDisplayName(String category) {
        try {
            SpendingCategory cat = SpendingCategory.valueOf(category.toUpperCase());
            return cat.getDisplayName();
        } catch (IllegalArgumentException e) {
            return "Other";
        }
    }

    /**
     * Get category short name for compact display
     */
    public String getCategoryShortName(String category) {
        try {
            SpendingCategory cat = SpendingCategory.valueOf(category.toUpperCase());
            return cat.getShortName();
        } catch (IllegalArgumentException e) {
            return "Other";
        }
    }

    /**
     * Get all available categories
     */
    public List<String> getAllCategories() {
        List<String> cats = new ArrayList<>();
        for (SpendingCategory cat : SpendingCategory.values()) {
            cats.add(cat.name());
        }
        return cats;
    }
}
