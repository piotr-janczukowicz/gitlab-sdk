package io.github.piotr_janczukowicz.gitlab.sdk;

import io.github.piotr_janczukowicz.http.MultipartFormData;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects the text fields of a project or group form in insertion order and turns them into the core's multipart form.
 * A field that was never put is never sent.
 */
final class ManagementForms {

    private final Map<String, String> fields;

    /** Creates a form with no field. */
    ManagementForms() {
        this.fields = new LinkedHashMap<>();
    }

    /**
     * Sets a field; a later value for the same name replaces the earlier one.
     *
     * @param name the form field name
     * @param value the value; sent as UTF-8 text
     */
    void put(final String name, final String value) {
        fields.put(name, value);
    }

    /**
     * Builds the form.
     *
     * @return the form with every field that was put
     * @throws IllegalStateException if no field was put
     */
    MultipartFormData build() {
        final MultipartFormData.Builder builder = MultipartFormData.builder();
        fields.forEach(builder::text);
        return builder.build();
    }
}
