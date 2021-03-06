package com.cyecize.summer.areas.validation.models;

import com.cyecize.summer.areas.validation.interfaces.BindingResult;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class BindingResultImpl implements BindingResult {

    private List<FieldError> errors;

    public BindingResultImpl() {
        this.errors = new ArrayList<>();
    }

    public BindingResultImpl(List<FieldError> errors) {
        this();
        if (errors != null) {
            this.errors = errors;
        }
    }

    @Override
    public void addNewError(FieldError fieldError) {
        this.errors.add(fieldError);
    }

    @Override
    public boolean hasErrors() {
        return this.errors.size() > 0;
    }

    @Override
    public List<FieldError> getFieldErrors(String field) {
        return this.errors.stream().filter(fe -> fe.getFieldName().equals(field)).collect(Collectors.toList());
    }

    @Override
    public List<FieldError> getErrors() {
        return this.errors;
    }
}
