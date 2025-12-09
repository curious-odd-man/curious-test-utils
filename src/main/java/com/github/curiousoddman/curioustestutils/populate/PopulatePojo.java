package com.github.curiousoddman.curioustestutils.populate;

import com.github.curiousoddman.curioustestutils.populate.impl.Context;
import com.github.curiousoddman.curioustestutils.populate.impl.DefaultValueGenerators;
import com.github.curiousoddman.curioustestutils.populate.impl.ValueGenerator;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.*;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;

@UtilityClass
@Slf4j
public class PopulatePojo {

    /**
     * Populates POJO with data using declared setters using random seed.
     *
     * @param pojo object to populate
     * @param <T>  type of object
     * @return populated pojo
     */
    public <T> T populatePojo(T pojo, ValueGenerator<?>... customGenerators) {
        return populatePojo(pojo, new Context(new SecureRandom().nextInt(), customGenerators));
    }

    /**
     * Populates POJO with data using declared setters.
     *
     * @param pojo object to populate
     * @param seed randomization seed
     * @param <T>  type of object
     * @return populated pojo
     */
    public <T> T populatePojo(T pojo, int seed, ValueGenerator<?>... customGenerators) {
        return populatePojo(pojo, new Context(seed, customGenerators));
    }

    @SneakyThrows
    public <T> T populatePojo(Class<T> pojoClass, Context context) {
        context.push("<init>", pojoClass, null);
        T obj;
        try {
            Optional<ValueGenerator<?>> generator = Stream.concat(
                            Arrays.stream(context.getCustomGenerators()),
                            DefaultValueGenerators.getDefaultGenerators().stream()
                    ).filter(valueGenerator -> valueGenerator.isApplicable(context))
                    .findFirst();
            if (generator.isPresent()) {
                return (T) generator.get().generateValue(context);
            } else {
                Constructor<T>[] declaredConstructors = (Constructor<T>[]) pojoClass.getDeclaredConstructors();
                Optional<Constructor<T>> constructor = Arrays
                        .stream(declaredConstructors)
                        .filter(declaredConstructor -> declaredConstructor.getParameterCount() == 0)
                        .findFirst();
                if (constructor.isPresent()) {
                    obj = constructor.get().newInstance();
                } else {
                    Constructor<T> constructorWithMostArguments = Arrays
                            .stream(declaredConstructors)
                            .reduce((first, last) -> first.getParameterCount() > last.getParameterCount() ? first : last)
                            .orElseThrow(() -> new IllegalStateException("No constructors found"));
                    Class<?>[] parameterTypes = constructorWithMostArguments.getParameterTypes();
                    Object[] args = Arrays.stream(parameterTypes)
                            .map(argType -> populatePojo(argType, context))
                            .toArray();
                    obj = constructorWithMostArguments.newInstance(args);
                }
            }
        } finally {
            context.pop();
        }
        return populatePojo(obj, context);
    }

    private <T> T populatePojo(T pojo, Context context) {
        Method[] declaredMethods = pojo.getClass().getDeclaredMethods();
        Map<String, List<Method>> settersByName = Arrays
                .stream(declaredMethods)
                .filter(method -> method.getName().startsWith("set"))
                .collect(groupingBy(Method::getName, LinkedHashMap::new, Collectors.toList()));

        List<Method> mostRestrictiveSetters = pickMostRestrictiveSetters(settersByName);
        mostRestrictiveSetters
                .forEach(setter -> {
                    int parameterCount = setter.getParameterCount();
                    if (parameterCount == 0) {
                        try {
                            setter.invoke(pojo);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalStateException("Cannot populate pojo", e);
                        }
                    } else if (parameterCount > 1) {
                        throw new IllegalStateException("Unexpected number of parameters in a setter method" + parameterCount);
                    } else {
                        try {
                            invokeWithSingleSetterArg(pojo, setter, context);
                        } catch (IllegalAccessException | InvocationTargetException e) {
                            throw new IllegalStateException("Cannot populate pojo", e);
                        }
                    }
                });

        return pojo;
    }

    /**
     * From two or more setters with the same name we need to pick the most restrictive type.
     * For example out of `setField(String)` amd `setField(Enum)` we need to pick the one with Enum.
     */
    private static List<Method> pickMostRestrictiveSetters(Map<String, List<Method>> settersByName) {
        List<Method> methods = new ArrayList<>(settersByName.size());
        for (List<Method> settersWithSameName : settersByName.values()) {
            Method mostRestrictive = settersWithSameName
                    .stream()
                    .reduce((first, last) -> {
                        // No args is more restrictive than any other
                        if (first.getParameterCount() == 0) {
                            return first;
                        } else if (last.getParameterCount() == 0) {
                            return last;
                        }

                        Class<?> aType = first.getParameterTypes()[0];
                        Class<?> bType = last.getParameterTypes()[0];

                        // Object type is the least restrictive
                        if (aType == Object.class) {
                            return last;
                        } else if (bType == Object.class) {
                            return first;
                        } else if (aType == String.class) {        // String type is less restrictive than other types
                            return last;
                        } else if (bType == String.class) {
                            return first;
                        }

                        // If there is known hierarchy of types, we can use isAssignableFrom to determine which one is more restrictive
                        return first.getParameterTypes()[0].isAssignableFrom(last.getParameterTypes()[0]) ? last : first;
                    })
                    .orElseThrow(() -> new IllegalStateException("Impossible! There should be at least one element"));
            methods.add(mostRestrictive);
        }
        return methods;
    }

    private <T> void invokeWithSingleSetterArg(T pojo, Method setter, Context context) throws IllegalAccessException, InvocationTargetException { // NOSONAR - reducing cognitive complexity actually increases it.
        Class<?> firstParameter = setter.getParameterTypes()[0];
        context.push(setter.getName(), firstParameter, getGenericElementType(setter));
        try {
            Optional<ValueGenerator<?>> generator = Stream.concat(
                            Arrays.stream(context.getCustomGenerators()),
                            DefaultValueGenerators.getDefaultGenerators().stream()
                    ).filter(valueGenerator -> valueGenerator.isApplicable(context))
                    .findFirst();
            if (generator.isPresent()) {
                Object obj = generator.get().generateValue(context);
                setter.invoke(pojo, obj);
            } else {
                try {
                    Object newSubObject = firstParameter.getConstructor().newInstance();
                    Object populatedSubObject = populatePojo(newSubObject, context);
                    setter.invoke(pojo, populatedSubObject);
                } catch (NoSuchMethodException | InstantiationException e) {
                    throw new IllegalStateException("Don't know how to generate value for setter at path '" + String.join("<-", context.getPath()) + "'");
                }
            }
        } finally {
            context.pop();
        }
    }

    private static Class<?> getGenericElementType(Method setter) {
        Type genericParameterType = setter.getGenericParameterTypes()[0];
        if (genericParameterType instanceof ParameterizedType parameterizedType) {
            return (Class<?>) parameterizedType.getActualTypeArguments()[0];
        }
        return null;
    }
}
