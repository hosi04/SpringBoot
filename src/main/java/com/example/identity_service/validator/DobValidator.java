package com.example.identity_service.validator;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {

    private int min;

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {

        if (Objects.isNull(value)) return true;

        long years = ChronoUnit.YEARS.between(value, LocalDate.now());

        return years >= min;
    }

    @Override
    public void initialize(DobConstraint constraintAnotation) {
        ConstraintValidator.super.initialize(constraintAnotation);
        min = constraintAnotation.min();
    }
}
