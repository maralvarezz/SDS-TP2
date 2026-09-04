package ar.edu.itba.sds.tp2.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Properties;

public final class FlockingConfigLoader {
    private static final String APPLICATION_PROPERTIES = "application.properties";

    private FlockingConfigLoader() {
    }

    public static FlockingConfig load(String[] args) {
        Properties properties = internalDefaults();
        loadApplicationProperties(properties);
        applyCliArgs(properties, args);

        return FlockingConfig.ofDensity(
                doubleProperty(properties, "flocking.rho"),
                doubleProperty(properties, "flocking.l"),
                doubleProperty(properties, "flocking.rc"),
                doubleProperty(properties, "flocking.v0"),
                doubleProperty(properties, "flocking.dt"),
                doubleProperty(properties, "flocking.eta"),
                intProperty(properties, "flocking.steps"),
                modelProperty(properties, "flocking.model"),
                optionalLongProperty(properties, "flocking.random-seed")
        );
    }

    private static Properties internalDefaults() {
        Properties properties = new Properties();
        properties.setProperty("flocking.rho", "4.0");
        properties.setProperty("flocking.l", "10.0");
        properties.setProperty("flocking.rc", "1.0");
        properties.setProperty("flocking.v0", "0.03");
        properties.setProperty("flocking.dt", "1.0");
        properties.setProperty("flocking.eta", "1.0");
        properties.setProperty("flocking.steps", "1000");
        properties.setProperty("flocking.model", "VICSEK");
        properties.setProperty("flocking.random-seed", "");
        return properties;
    }

    private static void loadApplicationProperties(Properties properties) {
        try (InputStream inputStream = FlockingConfigLoader.class.getClassLoader().getResourceAsStream(APPLICATION_PROPERTIES)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException e) {
            throw new IllegalArgumentException("No se pudo leer application.properties", e);
        }
    }

    private static void applyCliArgs(Properties properties, String[] args) {
        Map<String, String> cliMapping = cliMapping();
        for (String arg : args) {
            if (!arg.startsWith("--") || !arg.contains("=")) {
                throw new IllegalArgumentException("Parametro CLI invalido: " + arg + ". Usar formato --clave=valor");
            }
            String[] parts = arg.substring(2).split("=", 2);
            String propertyKey = cliMapping.get(parts[0]);
            if (propertyKey == null) {
                throw new IllegalArgumentException("Parametro CLI desconocido: --" + parts[0]);
            }
            properties.setProperty(propertyKey, parts.length == 2 ? parts[1] : "");
        }
    }

    private static Map<String, String> cliMapping() {
        Map<String, String> mapping = new HashMap<>();
        mapping.put("rho", "flocking.rho");
        mapping.put("l", "flocking.l");
        mapping.put("rc", "flocking.rc");
        mapping.put("v0", "flocking.v0");
        mapping.put("dt", "flocking.dt");
        mapping.put("eta", "flocking.eta");
        mapping.put("steps", "flocking.steps");
        mapping.put("model", "flocking.model");
        mapping.put("random-seed", "flocking.random-seed");
        return mapping;
    }

    private static int intProperty(Properties properties, String key) {
        try {
            return Integer.parseInt(properties.getProperty(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser entera", e);
        }
    }

    private static double doubleProperty(Properties properties, String key) {
        try {
            return Double.parseDouble(properties.getProperty(key));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser numerica", e);
        }
    }

    private static FlockingModel modelProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        try {
            return FlockingModel.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser VICSEK o VOTER, recibido: " + value);
        }
    }

    private static OptionalLong optionalLongProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("La propiedad " + key + " debe ser long o vacia", e);
        }
    }
}
