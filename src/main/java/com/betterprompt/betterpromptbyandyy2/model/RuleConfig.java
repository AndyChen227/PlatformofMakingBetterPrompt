package com.betterprompt.betterpromptbyandyy2.model;

import java.util.Map;

/**
 * Configuration for a single Rule: whether it's enabled and any named parameters.
 * Deserialized from the incoming JSON request per rule ID.
 */
public class RuleConfig {

    private boolean enabled;
    private Map<String, Object> params;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, Object> getParams() { return params; }
    public void setParams(Map<String, Object> params) { this.params = params; }

    /**
     * Helper: safely read an integer param with a default fallback.
     * Jackson deserializes JSON numbers as Integer, so we handle Number generically.
     */
    public int getIntParam(String key, int defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object val = params.get(key);
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return defaultValue; }
    }

    /**
     * Helper: safely read a String param with a default fallback.
     */
    public String getStringParam(String key, String defaultValue) {
        if (params == null || !params.containsKey(key)) return defaultValue;
        Object val = params.get(key);
        return val != null ? val.toString() : defaultValue;
    }
}
