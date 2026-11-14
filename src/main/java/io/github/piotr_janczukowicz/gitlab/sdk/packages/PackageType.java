package io.github.piotr_janczukowicz.gitlab.sdk.packages;

import static io.github.piotr_janczukowicz.util.ValidationUtils.requireNonNull;

/**
 * The type of a package, which is the ecosystem it belongs to. The values are the ones the core accepts as the
 * {@code package_type} of the package lists, with the same constant names. A type in a response that is not listed here
 * makes {@link ProjectPackage#type()} throw; {@link ProjectPackage#typeName()} gives the raw name.
 */
public enum PackageType {
    /** A Maven package. */
    MAVEN("maven"),
    /** An npm package. */
    NPM("npm"),
    /** A Conan package. */
    CONAN("conan"),
    /** A NuGet package. */
    NUGET("nuget"),
    /** A PyPI package. */
    PYPI("pypi"),
    /** A Composer package. */
    COMPOSER("composer"),
    /** A generic package. */
    GENERIC("generic"),
    /** A Go module. */
    GOLANG("golang"),
    /** A Debian package. */
    DEBIAN("debian"),
    /** A RubyGems package. */
    RUBYGEMS("rubygems"),
    /** A Helm chart. */
    HELM("helm"),
    /** A Terraform module. */
    TERRAFORM_MODULE("terraform_module"),
    /** An RPM package. */
    RPM("rpm"),
    /** A machine learning model. */
    ML_MODEL("ml_model"),
    /** A Cargo crate. */
    CARGO("cargo"),
    /** A Swift package. */
    SWIFT("swift");

    private final String value;

    PackageType(final String value) {
        this.value = value;
    }

    /**
     * Returns the name of the type as GitLab spells it, such as {@code terraform_module}.
     *
     * @return the name GitLab uses
     */
    public String value() {
        return value;
    }

    /**
     * Finds the type GitLab spells with a name.
     *
     * @param value the name as GitLab spells it
     * @return the type
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if the SDK does not know the name
     */
    public static PackageType from(final String value) {
        requireNonNull(value, "value");
        for (final PackageType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown package type: " + value);
    }
}
