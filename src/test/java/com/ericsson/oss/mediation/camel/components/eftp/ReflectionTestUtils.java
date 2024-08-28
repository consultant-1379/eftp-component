/*------------------------------------------------------------------------------
 *******************************************************************************
 * COPYRIGHT Ericsson 2012
 *
 * The copyright to the computer program(s) herein is the property of
 * Ericsson Inc. The programs may be used and/or copied only with written
 * permission from Ericsson Inc. or in accordance with the terms and
 * conditions stipulated in the agreement/contract under which the
 * program(s) have been supplied.
 *******************************************************************************
 *----------------------------------------------------------------------------*/
package com.ericsson.oss.mediation.camel.components.eftp;

import java.lang.reflect.Field;

public class ReflectionTestUtils {
    public static Field findNonPrimitiveField(final Class<?> classToSearch,
            final Class<?> fieldType) throws SecurityException {
        for (Field field : getFields(classToSearch, fieldType)) {
            field.setAccessible(true);
            final Class<?> clazz = field.getType();
            if (clazz.getName().equals(fieldType.getName())) {
                return field;
            }
        }

        throw new IllegalArgumentException("The field of type " + fieldType
                + "was not found");
    }

    public static Field[] getFields(final Class<?> classToSearch,
            final Class<?> fieldType) throws SecurityException {
        if (fieldType == null) {
            throw new IllegalArgumentException(
                    "The fieldType Class must not be null");
        }

        final Field[] fields = classToSearch.getDeclaredFields();
        if (fields.length == 0) {
            throw new IllegalArgumentException("The Class "
                    + classToSearch.getName() + "must contain fields");
        }
        return fields;
    }

    public static void setNonPrimitiveField(final Class<?> classToSearch,
            final Class<?> fieldType, final Object classInstance,
            final Object value) throws SecurityException,
            IllegalAccessException {

        final Field field = findNonPrimitiveField(classToSearch, fieldType);
        field.set(classInstance, value);
    }

    public static void setPrimitiveField(final Class<?> classToSearch,
            final Class<?> fieldType, final String fieldName,
            final Object classInstance, final Object value)
            throws SecurityException, IllegalAccessException {

        for (Field field : getFields(classToSearch, fieldType)) {
            field.setAccessible(true);
            if (field.getName().equalsIgnoreCase(fieldName)) {
                field.set(classInstance, value);
                return;
            }
        }
        throw new IllegalArgumentException("The field name " + fieldName
                + "was not found");
    }
}
