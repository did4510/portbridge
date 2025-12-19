Authorship and License

Please replace the placeholder values in `gradle.properties` with the correct information before publishing:

- `mod_authors` - set to a comma-separated list of author names or names with contact info. Example:

  mod_authors=Alice Example <alice@example.com>, Bob Example <bob@example.com>

- `mod_license` - set to a license identifier such as `MIT`, `Apache-2.0`, or `All Rights Reserved`.

The `generateModMetadata` Gradle task will expand these values into `neoforge.mods.toml` during the build. Ensure you also include the matching `LICENSE` file at the repository root for clarity.
