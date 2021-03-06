/*
 * Copyright (c) 2016-2019 Intel Corporation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intel.rsd.nodecomposer.utils;

import net.sf.corn.cps.ClassFilter;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toSet;
import static net.sf.corn.cps.CPScanner.scanClasses;

public final class ClassGatherer {
    private ClassGatherer() {
    }

    public static Set<Class<?>> getAllClassesByPackage(String packageName) {
        return new HashSet<>(scanClasses(new ClassFilter().packageName(packageName)));
    }

    public static Set<Class<?>> getAllClassesByPackageAndAnnotation(String packageName, Class<? extends Annotation> annotation) {
        return new HashSet<>(scanClasses(new ClassFilter()
            .packageName(packageName)
            .annotation(annotation)));
    }

    public static Set<Class<?>> getAllClassesByPackageAndSuperTypeWithTheirInnerClasses(String packageName, Class<?> superClass) {
        return scanClasses(new ClassFilter()
            .packageName(packageName)
            .superClass(superClass))
            .stream()
            .flatMap(clazz -> Stream.concat(Stream.of(clazz), getDeclaredClassesRecursively(clazz, new HashSet<>()).stream()))
            .collect(toSet());
    }

    public static Set<Class<?>> getAllFieldsTypesDeeplyFromClassesDeclaredInPackage(Set<Class<?>> classes, String packageName) {
        Pattern packageNamePattern = Pattern.compile(packageName);

        Set<Class<?>> foundClasses = new HashSet<>();
        for (Class<?> clazz : classes) {
            getAllFieldsDeeplyFromClassDeclaredInPackage(clazz, packageNamePattern, foundClasses);
        }

        return foundClasses;
    }

    public static Class<?> extractTypeOfField(Field field) {
        if (field.getGenericType() instanceof ParameterizedType) {
            return (Class<?>) extractTypeFromParameterizedType(field.getGenericType());
        }

        return field.getType();
    }

    private static Set<Class<?>> getDeclaredClassesRecursively(Class<?> clazz, Set<Class<?>> foundDeclaredClasses) {
        stream(clazz.getDeclaredClasses())
            .filter(declaredClass -> !foundDeclaredClasses.contains(declaredClass))
            .forEach(declaredClass -> {
                foundDeclaredClasses.add(declaredClass);
                getDeclaredClassesRecursively(declaredClass, foundDeclaredClasses);
            });

        return foundDeclaredClasses;
    }

    private static void getAllFieldsDeeplyFromClassDeclaredInPackage(Class<?> clazz, Pattern packageNamePattern, Set<Class<?>> foundClasses) {
        if (foundClasses.contains(clazz)) {
            return;
        }
        foundClasses.add(clazz);

        stream(clazz.getDeclaredFields())
            .map(ClassGatherer::extractTypeOfField)
            .filter(fieldType -> packageNamePattern.matcher(fieldType.getName()).find())
            .forEach(fieldType -> getAllFieldsDeeplyFromClassDeclaredInPackage(fieldType, packageNamePattern, foundClasses));
    }

    private static Type extractTypeFromParameterizedType(Type genericType) {
        Type actualType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        if (actualType instanceof Class) {
            return actualType;
        } else {
            return extractTypeFromParameterizedType(actualType);
        }
    }
}
