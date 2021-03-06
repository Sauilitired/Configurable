package com.intellectualsites.configurable;

import com.intellectualsites.configurable.annotations.ConfigSection;
import com.intellectualsites.configurable.annotations.ConfigValue;
import com.intellectualsites.configurable.annotations.Configuration;
import com.intellectualsites.configurable.exception.ConfigurationFactoryException;
import com.intellectualsites.configurable.implementation.JsonConfig;
import com.intellectualsites.configurable.implementation.TomlConfig;
import com.intellectualsites.configurable.implementation.YamlConfig;
import com.intellectualsites.configurable.reflection.FieldProperty;
import com.intellectualsites.configurable.reflection.IField;
import lombok.SneakyThrows;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.HashMap;

public final class ConfigurationFactory {

    /**
     * A default name for nameable configuration nodes.
     * Will default to the name of the implementing class
     */
    public static final String DEFAULT_CONFIG_NAME = "__CLASS__";

    @SneakyThrows
    public static <T> Config<T> load(final Class<T> clazz, File folder, Object... constructorArguments) {
        T instance;
        try {
            instance = getInstance(clazz, constructorArguments);
        } catch (final Exception e) {
            throw new ConfigurationFactoryException(clazz, "Failed to construct an instance", e);
        }
        return load(clazz, instance, folder);
    }
    
    public static <T> Config<T> load(final Class<T> clazz, Object ... constructorArguments) {
        return load(clazz, new File("."), constructorArguments);
    }

    @SneakyThrows
    public static <T> Config<T> load(final Class<T> clazz, final T instance, File folder) {
        Config<T> config;
        try {
            config = from(clazz, instance);
            if (config != null) {
                config.read(folder);
            } else {
                throw new NullPointerException("config");
            }
        } catch (ConfigurationFactoryException e) {
            new ConfigurationFactoryException(clazz, "Failed to load config, using defaults instead", e).printStackTrace();
            config = new Config<T>(instance) {
                @Override
                protected void readInternal(File file) {
                    throw new UnsupportedOperationException("This is a broken config");
                }

                @Override
                protected void saveInternal(File file) {
                    throw new UnsupportedOperationException("This is a broken config");
                }

                @Override
                public ConfigurationImplementation getImplementation() {
                    throw new UnsupportedOperationException("This is a broken config");
                }
            };
        }
        return config;
    }
    
    public static <T> Config<T> load(final Class<T> clazz, final T instance) {
        return load(clazz, instance, new File("."));
    }


    private static <T> T getInstance(final Class<T> clazz, Object ... constructorArguments) throws NoSuchMethodException, IllegalAccessException, InstantiationException, InvocationTargetException {
        final T instance;
        if (constructorArguments.length == 0) {
            instance = clazz.newInstance();
        } else {
            final Class<?>[] classes = new Class[constructorArguments.length];
            for (int i = 0; i < constructorArguments.length; i++) {
                classes[i] = constructorArguments[i].getClass();
            }
            instance = clazz.getDeclaredConstructor(classes)
                    .newInstance(constructorArguments);
        }
        return instance;
    }
    
    public static <T> Config<T> from(final Class<T> clazz, Object ... constructorArguments)
            throws ConfigurationFactoryException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        final T instance = getInstance(clazz, constructorArguments);
        return from(clazz, getInstance(clazz, constructorArguments));
    }

    public static <T> Config<T> from(final Class<T> clazz, T instance) throws ConfigurationFactoryException {
        if (clazz == null) {
            throw new ConfigurationFactoryException("Specified class was null",
                    new NullPointerException("clazz"));
        }
        if (clazz.isAnnotationPresent(Configuration.class)) {
            final Configuration configuration = clazz.getAnnotation(Configuration.class);
            final String configName = configuration.name().equals(DEFAULT_CONFIG_NAME) ?
                    clazz.getSimpleName() : configuration.name();
            final ConfigurationImplementation implementation = configuration.implementation();
            final HashMap<String, IField<T>> fields = new HashMap<>();
            for (final Field field : clazz.getDeclaredFields()) {
                if (configuration.annotationMode() == AnnotationMode.REQUIRED && !field.isAnnotationPresent(ConfigValue.class)) {
                     continue;
                }
                if (Modifier.isTransient(field.getModifiers())) {
                    continue;
                }
                final IField<T> ifield = new IField<>(clazz).named(field.getName())
                        .withProperty(FieldProperty.ACCESS_GRANT);
                if (Modifier.isFinal(field.getModifiers())) {
                    new RuntimeException(field.getName() + " in config " + configName + " is final - It's not supported as of now. Skipping field.").printStackTrace();
                    continue;
                }
                if (Modifier.isStatic(field.getModifiers())) {
                    ifield.withProperty(FieldProperty.STATIC);
                } else {
                    ifield.fromInstance(instance);
                }
                fields.put(field.getName(), ifield);
            }
            for (final Class<?> inner : clazz.getDeclaredClasses()) {
                if (!inner.isAnnotationPresent(ConfigSection.class)) {
                    continue;
                }
                final ConfigSection configSection = inner.getAnnotation(ConfigSection.class);
                final String sectionName = configSection.name().equals(DEFAULT_CONFIG_NAME) ?
                        inner.getSimpleName() : configSection.name();
                Object innerInstance = null;
                try {
                    if (Modifier.isStatic(inner.getModifiers())) {
                        innerInstance = inner.newInstance();
                    } else {
                        innerInstance = new IField<>(clazz).fromInstance(instance).named(sectionName)
                                .withProperties(FieldProperty.CONSTANT, FieldProperty.ACCESS_GRANT).getValue();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                for (final Field field : inner.getDeclaredFields()) {
                    if (field.getName().equals("this$0")) {
                        continue;
                    }
                    if ((configSection.annotationMode() == AnnotationMode.REQUIRED ||
                            (configSection.annotationMode() == AnnotationMode.INHERIT && configuration.annotationMode() == AnnotationMode.REQUIRED)) &&
                            !field.isAnnotationPresent(ConfigValue.class)) {
                         continue;
                    }
                    if (Modifier.isTransient(field.getModifiers())) {
                        continue;
                    }
                    final IField ifield = new IField<>(inner).named(field.getName())
                            .withProperty(FieldProperty.ACCESS_GRANT);
                    if (Modifier.isFinal(field.getModifiers())) {
                        new RuntimeException(field.getName() + " in config " + configName + " and section " + sectionName  +" is final - It's not supported as of now. Skipping field.").printStackTrace();
                        continue;
                    }
                    if (Modifier.isStatic(field.getModifiers())) {
                        ifield.withProperty(FieldProperty.STATIC);
                    } else {
                        ifield.fromInstance(innerInstance);
                    }
                    fields.put(sectionName + "." + field.getName(), ifield);
                }
            }
            switch (implementation) {
                case JSON:
                    return new JsonConfig<>(configName, clazz, instance, fields);
                case YAML:
                    return new YamlConfig<>(configName, clazz, instance, fields);
                case TOML:
                    return new TomlConfig<>(configName, clazz, instance, fields);
                default: break; // Will never happen
            }
        } else {
            throw new ConfigurationFactoryException(clazz, "No @Configuration annotation present");
        }
        return null; // Shouldn't happen!
    }

}
