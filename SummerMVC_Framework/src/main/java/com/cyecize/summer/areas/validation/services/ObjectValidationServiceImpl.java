package com.cyecize.summer.areas.validation.services;

import com.cyecize.summer.areas.scanning.services.DependencyContainer;
import com.cyecize.summer.areas.validation.annotations.Constraint;
import com.cyecize.summer.areas.validation.constraints.*;
import com.cyecize.summer.areas.validation.exceptions.ValidationException;
import com.cyecize.summer.areas.validation.interfaces.BindingResult;
import com.cyecize.summer.areas.validation.interfaces.ConstraintValidator;
import com.cyecize.summer.areas.validation.models.FieldError;
import com.cyecize.summer.common.annotations.Component;
import com.cyecize.summer.common.enums.ServiceLifeSpan;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ObjectValidationServiceImpl implements ObjectValidationService {

    private static final String NO_MESSAGE_PRESENT_FORMAT = "No message method present in annotation \"%s\".";

    private static final String ADD_ANNOTATION_TO_CLASS_FORMAT = "Validator \"%s\" not annotated with @Component";

    private Map<Class<?>, ConstraintValidator> classValidatorMap;

    private final DependencyContainer dependencyContainer;

    public ObjectValidationServiceImpl(Set<Object> validators, DependencyContainer dependencyContainer) {
        this.dependencyContainer = dependencyContainer;
        this.setClassValidatorMap(validators);
    }

    /**
     * Iterate all fields of the given object and for each field perform validation.
     */
    @Override
    public void validateBindingModel(Object bindingModel, BindingResult bindingResult) {
        try {
            for (Field declaredField : bindingModel.getClass().getDeclaredFields()) {
                this.validateField(declaredField, bindingModel, bindingResult);
            }
        } catch (Throwable th) {
            throw new ValidationException(th.getMessage(), th);
        }
    }

    @SuppressWarnings("unchecked")
    /**
     * Iterates all annotations for the given field.
     * If an annotation is annotated with @Constraint, get the constraint validator.
     * If the constraintValidator is null, throw ValidationException.
     * Reload the validator if lifeSpan == REQUEST
     *
     * If the validator returns false, add new FieldError to the bindingResult.
     */
    private void validateField(Field field, Object bindingModel, BindingResult bindingResult) {
        try {
            field.setAccessible(true);
            Object fieldVal = field.get(bindingModel);

            for (Annotation currentAnnotation : field.getDeclaredAnnotations()) {
                if (!currentAnnotation.annotationType().isAnnotationPresent(Constraint.class)) {
                    continue;
                }

                Constraint constraint = currentAnnotation.annotationType().getAnnotation(Constraint.class);
                ConstraintValidator validator = this.classValidatorMap.get(constraint.validatedBy());

                if (validator == null || constraint.validatedBy().getAnnotation(Component.class) == null) {
                    throw new ValidationException(String.format(ADD_ANNOTATION_TO_CLASS_FORMAT, constraint.validatedBy().getName()));
                }

                validator = this.dependencyContainer.reloadComponent(validator, ServiceLifeSpan.REQUEST);

                validator.initialize(currentAnnotation);

                if (validator.isValid(fieldVal, bindingModel)) {
                    continue;
                }

                bindingResult.addNewError(new FieldError(bindingModel.getClass().getName(), field.getName(), this.getAnnotationMessage(currentAnnotation), fieldVal));
            }

        } catch (IllegalAccessException e) {
            throw new ValidationException(e.getMessage(), e);
        }
    }

    /**
     * Gets the message method of a given annotation using reflection.
     *
     * @throws ValidationException if the annotation is missing the message method.
     */
    private String getAnnotationMessage(Annotation annotation) {
        try {
            Method message = annotation.annotationType().getDeclaredMethod("message");
            message.setAccessible(true);
            return (String) message.invoke(annotation);
        } catch (NoSuchMethodException e) {
            throw new ValidationException(String.format(NO_MESSAGE_PRESENT_FORMAT, annotation.getClass().getName()));
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new ValidationException(e.getMessage(), e);
        }
    }

    /**
     * Converts the set of objects to a map of Class and ConstraintValidator
     * Then calls the addPlatformValidators method.
     */
    private void setClassValidatorMap(Set<Object> validators) {
        this.classValidatorMap = new HashMap<>();
        for (Object validator : validators) {
            this.classValidatorMap.put(validator.getClass(), (ConstraintValidator) validator);
        }
        this.addPlatformValidators();
    }

    /**
     * Add Platform validators here
     */
    private void addPlatformValidators() {
        this.classValidatorMap.put(NotNullConstraint.class, new NotNullConstraint());
        this.classValidatorMap.put(NotEmptyConstraint.class, new NotEmptyConstraint());
        this.classValidatorMap.put(FieldMatchConstraint.class, new FieldMatchConstraint());
        this.classValidatorMap.put(MaxLengthConstraint.class, new MaxLengthConstraint());
        this.classValidatorMap.put(MinLengthConstraint.class, new MinLengthConstraint());
        this.classValidatorMap.put(MaxConstraint.class, new MaxConstraint());
        this.classValidatorMap.put(MinConstraint.class, new MinConstraint());
        this.classValidatorMap.put(RegExConstraint.class, new RegExConstraint());
        this.classValidatorMap.put(MediaTypeConstraint.class, new MediaTypeConstraint());
    }
}
