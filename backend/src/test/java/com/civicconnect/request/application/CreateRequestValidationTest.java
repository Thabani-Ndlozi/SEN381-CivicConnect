package com.civicconnect.request.application;

import com.civicconnect.request.domain.RequestCategory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** BB-01..BB-07: equivalence partitioning and length boundary value analysis. */
class CreateRequestValidationTest {
    private static final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
    private CreateRequestCommand cmd(String title, String description, RequestCategory category) {
        return new CreateRequestCommand(title, description, category);
    }
    @Test void validTypicalPartition() { assertTrue(validator.validate(cmd("Broken lamp", "Near front gate", RequestCategory.IT_SUPPORT)).isEmpty()); }
    @Test void titleBoundaryOneCharacterAccepted() { assertTrue(validator.validate(cmd("A", "Description", RequestCategory.OTHER)).isEmpty()); }
    @Test void titleBoundary160Accepted() { assertTrue(validator.validate(cmd("A".repeat(160), "Description", RequestCategory.OTHER)).isEmpty()); }
    @Test void titleBoundary161Rejected() { assertFalse(validator.validate(cmd("A".repeat(161), "Description", RequestCategory.OTHER)).isEmpty()); }
    @Test void whitespaceTitleRejected() { assertFalse(validator.validate(cmd("  ", "Description", RequestCategory.OTHER)).isEmpty()); }
    @Test void missingCategoryRejected() { assertFalse(validator.validate(cmd("Valid", "Description", null)).isEmpty()); }
    @Test void description4001Rejected() { assertFalse(validator.validate(cmd("Valid", "Z".repeat(4001), RequestCategory.OTHER)).isEmpty()); }
    @Test void description4000Accepted() { assertTrue(validator.validate(cmd("Valid", "Z".repeat(4000), RequestCategory.OTHER)).isEmpty()); }
}
