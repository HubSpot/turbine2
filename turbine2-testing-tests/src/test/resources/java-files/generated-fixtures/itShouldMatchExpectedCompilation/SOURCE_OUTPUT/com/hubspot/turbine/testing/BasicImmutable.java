package com.hubspot.turbine.testing;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import com.hubspot.immutables.validation.InvalidImmutableStateException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import javax.annotation.concurrent.NotThreadSafe;
import org.immutables.value.Generated;

/**
 * Immutable implementation of {@link BasicImmutableIF}.
 * <p>
 * Use the builder to create immutable instances:
 * {@code BasicImmutable.builder()}.
 */
@Generated(from = "BasicImmutableIF", generator = "Immutables")
@SuppressWarnings({"all"})
@SuppressFBWarnings
@ParametersAreNonnullByDefault
@javax.annotation.processing.Generated("org.immutables.processor.ProxyProcessor")
@Immutable
public final class BasicImmutable implements BasicImmutableIF {
  private final String string;

  private BasicImmutable(String string) {
    this.string = string;
  }

  /**
   * @return The value of the {@code string} attribute
   */
  @JsonProperty
  @Override
  public String getString() {
    return string;
  }

  /**
   * Copy the current immutable object by setting a value for the {@link BasicImmutableIF#getString() string} attribute.
   * An equals check used to prevent copying of the same value by returning {@code this}.
   * @param value A new value for string
   * @return A modified copy of the {@code this} object
   */
  public final BasicImmutable withString(String value) {
    String newValue = Objects.requireNonNull(value, "string");
    if (this.string.equals(newValue)) return this;
    return new BasicImmutable(newValue);
  }

  /**
   * This instance is equal to all instances of {@code BasicImmutable} that have equal attribute values.
   * @return {@code true} if {@code this} is equal to {@code another} instance
   */
  @Override
  public boolean equals(@Nullable Object another) {
    if (this == another) return true;
    return another instanceof BasicImmutable
        && equalTo(0, (BasicImmutable) another);
  }

  private boolean equalTo(int synthetic, BasicImmutable another) {
    return string.equals(another.string);
  }

  /**
   * Computes a hash code from attributes: {@code string}.
   * @return hashCode value
   */
  @Override
  public int hashCode() {
    int h = 5381;
    h += (h << 5) + string.hashCode();
    return h;
  }

  /**
   * Prints the immutable value {@code BasicImmutable} with attribute values.
   * @return A string representation of the value
   */
  @Override
  public String toString() {
    return MoreObjects.toStringHelper("BasicImmutable")
        .omitNullValues()
        .add("string", string)
        .toString();
  }

  /**
   * Utility type used to correctly read immutable object from JSON representation.
   * @deprecated Do not use this type directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Generated(from = "BasicImmutableIF", generator = "Immutables")
  @Deprecated
  @JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.NONE)
  static final class Json implements BasicImmutableIF {
    @Nullable String string;
    @JsonProperty
    public void setString(String string) {
      this.string = string;
    }
    @Override
    public String getString() { throw new UnsupportedOperationException(); }
  }

  /**
   * @param json A JSON-bindable data structure
   * @return An immutable value type
   * @deprecated Do not use this method directly, it exists only for the <em>Jackson</em>-binding infrastructure
   */
  @Deprecated
  @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
  static BasicImmutable fromJson(Json json) {
    BasicImmutable.Builder builder = BasicImmutable.builder();
    if (json.string != null) {
      builder.setString(json.string);
    }
    return builder.build();
  }

  /**
   * Creates an immutable copy of a {@link BasicImmutableIF} value.
   * Uses accessors to get values to initialize the new immutable instance.
   * If an instance is already immutable, it is returned as is.
   * @param instance The instance to copy
   * @return A copied immutable BasicImmutable instance
   */
  public static BasicImmutable copyOf(BasicImmutableIF instance) {
    if (instance instanceof BasicImmutable) {
      return (BasicImmutable) instance;
    }
    return BasicImmutable.builder()
        .from(instance)
        .build();
  }

  /**
   * Creates a builder for {@link BasicImmutable BasicImmutable}.
   * <pre>
   * BasicImmutable.builder()
   *    .setString(String) // required {@link BasicImmutableIF#getString() string}
   *    .build();
   * </pre>
   * @return A new BasicImmutable builder
   */
  public static BasicImmutable.Builder builder() {
    return new BasicImmutable.Builder();
  }

  /**
   * Builds instances of type {@link BasicImmutable BasicImmutable}.
   * Initialize attributes and then invoke the {@link #build()} method to create an
   * immutable instance.
   * <p><em>{@code Builder} is not thread-safe and generally should not be stored in a field or collection,
   * but instead used immediately to create instances.</em>
   */
  @Generated(from = "BasicImmutableIF", generator = "Immutables")
  @NotThreadSafe
  public static final class Builder {
    private static final long INIT_BIT_STRING = 0x1L;
    private long initBits = 0x1L;

    private @Nullable String string;

    private Builder() {
    }

    /**
     * Fill a builder with attribute values from the provided {@code BasicImmutableIF} instance.
     * Regular attribute values will be replaced with those from the given instance.
     * Absent optional values will not replace present values.
     * @param instance The instance from which to copy values
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder from(BasicImmutableIF instance) {
      Objects.requireNonNull(instance, "instance");
      this.setString(instance.getString());
      return this;
    }

    /**
     * Initializes the value for the {@link BasicImmutableIF#getString() string} attribute.
     * @param string The value for string 
     * @return {@code this} builder for use in a chained invocation
     */
    public final Builder setString(String string) {
      this.string = Objects.requireNonNull(string, "string");
      initBits &= ~INIT_BIT_STRING;
      return this;
    }

    /**
     * Builds a new {@link BasicImmutable BasicImmutable}.
     * @return An immutable instance of BasicImmutable
     * @throws com.hubspot.immutables.validation.InvalidImmutableStateException if any required attributes are missing
     */
    public BasicImmutable build() {
      checkRequiredAttributes();
      return new BasicImmutable(string);
    }

    private boolean stringIsSet() {
      return (initBits & INIT_BIT_STRING) == 0;
    }

    private void checkRequiredAttributes() {
      if (initBits != 0) {
        throw new InvalidImmutableStateException(formatRequiredAttributesMessage());
      }
    }

    private String formatRequiredAttributesMessage() {
      List<String> attributes = new ArrayList<>();
      if (!stringIsSet()) attributes.add("string");
      return "Cannot build BasicImmutable, some of required attributes are not set " + attributes;
    }
  }
}
