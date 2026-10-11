package com.swiftfaze.veil.mods;

import com.google.gson.JsonElement;
import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaException;
import com.networknt.schema.SchemaLocation;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.swiftfaze.veil.exceptions.ModLoadException;
import com.swiftfaze.veil.exceptions.SchemaRegistryInitializationException;

/**
 * Validates mod content files (mod.json, tiles, buildings, etc.) against JSON schemas
 * committed under docs/schemas/. Produces field-level error messages naming the file,
 * the offending field/path, and what was expected vs. found.
 */
public final class ModSchemaValidator {

	private static final Logger logger = LoggerFactory.getLogger(ModSchemaValidator.class);

	private static final String SCHEMAS_BASE = "classpath:schemas";

	private static final Map<String, String> SCHEMA_PATHS = Map.ofEntries(
			Map.entry("mod", "mod.schema.json"),
			Map.entry("stats", "stats.schema.json"),
			Map.entry("tile", "tile.schema.json"),
			Map.entry("building", "building.schema.json"),
			Map.entry("class", "class.schema.json"),
			Map.entry("item", "item.schema.json"),
			Map.entry("quest", "quest.schema.json"),
			Map.entry("object", "object.schema.json"),
			Map.entry("npc", "npc.schema.json"),
			Map.entry("dialogue", "dialogue.schema.json"),
			Map.entry("theme", "theme.schema.json")
	);

	private ModSchemaValidator() {
	}

	/**
	 * Initialization-on-demand holder: the JVM's class-loading guarantees give
	 * lazy, thread-safe initialization without needing a mutable static field
	 * or double-checked locking.
	 */
	private static final class Holder {
		private static final SchemaRegistry INSTANCE = initializeRegistry();

		private Holder() {
		}
	}

	private static SchemaRegistry getSchemaRegistry() {
		return Holder.INSTANCE;
	}

	private static SchemaRegistry initializeRegistry() {
		try {
			SchemaRegistry registry = SchemaRegistry
					.withDefaultDialect(SpecificationVersion.DRAFT_2020_12,
							builder -> builder.schemaIdResolvers(schemaIdResolvers -> schemaIdResolvers
									.mapPrefix("https://veil.local/schema", SCHEMAS_BASE)));

			// Pre-load all schemas
			for (String schemaPath : SCHEMA_PATHS.values()) {
				SchemaLocation location = SchemaLocation
						.of("https://veil.local/schema/" + schemaPath);
				Schema schema = registry.getSchema(location);
				schema.initializeValidators();
			}

			return registry;
		} catch (SchemaException e) {
			logger.error("Failed to initialize JSON schema registry", e);
			throw new SchemaRegistryInitializationException("Failed to initialize JSON schema registry", e);
		}
	}

	/**
	 * Validates a mod file against its schema.
	 *
	 * @param schemaType the schema type (e.g., "mod", "tile", "building")
	 * @param jsonElement the parsed JSON element to validate
	 * @param filePath the file being validated (for error messages)
	 * @throws ModLoadException if validation fails with field-level diagnostics
	 */
	public static void validate(String schemaType, JsonElement jsonElement, Path filePath) {
		String schemaFileName = requireSchemaFileName(schemaType);
		List<Error> errors = runSchemaValidation(schemaFileName, jsonElement, filePath);
		if (!errors.isEmpty()) {
			throw new ModLoadException(buildValidationErrorMessage(schemaType, filePath, errors));
		}
	}

	private static String requireSchemaFileName(String schemaType) {
		String schemaFileName = SCHEMA_PATHS.get(schemaType);
		if (schemaFileName == null) {
			throw new IllegalArgumentException("Unknown schema type: " + schemaType);
		}
		return schemaFileName;
	}

	private static List<Error> runSchemaValidation(String schemaFileName, JsonElement jsonElement, Path filePath) {
		try {
			SchemaLocation location = SchemaLocation.of("https://veil.local/schema/" + schemaFileName);
			Schema schema = getSchemaRegistry().getSchema(location);

			// Validate using Jackson's InputFormat.JSON (json-schema-validator's native format)
			return schema.validate(jsonElement.toString(), InputFormat.JSON);
		} catch (SchemaException e) {
			throw new ModLoadException("Schema validation error for " + filePath, e);
		}
	}

	private static String buildValidationErrorMessage(String schemaType, Path filePath, List<Error> errors) {
		StringBuilder message = new StringBuilder();
		message.append("Failed to load ").append(schemaType).append(" from file: ").append(filePath);

		for (Error error : errors) {
			message.append("\n  ");
			String instancePath = error.getInstanceLocation().toString();
			if (instancePath.isEmpty() || instancePath.equals("$")) {
				message.append("(root) ");
			} else {
				message.append(instancePath).append(" ");
			}
			message.append("- ").append(error.getMessage());
		}

		return message.toString();
	}
}
