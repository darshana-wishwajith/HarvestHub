package com.harvesthub.backend.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Crop catalogue (existing "crops" collection, 41 entries). Crops added by farmers through
 * "Can't find your harvest? Add New" are stored with verified = false until an officer reviews them.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "crops")
public class Crop {
    @Id
    private String id;
    private String name;
    private String category;
    private String defaultUnit;
    private Double standardPrice;
    /** null (existing catalogue entries) or true = verified; false = added by a farmer, pending review. */
    private Boolean verified;
    private String addedByUserId;
    private String createdAt;

    public boolean isPending() {
        return Boolean.FALSE.equals(verified);
    }
}
