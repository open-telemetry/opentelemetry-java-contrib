/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class RecipeManager {
  private final Object recipeLock = new Object();
  private final List<Field> constantFields;
  @Nullable private RequestRecipe previousRecipe = null;
  @Nullable private RecipeBuilder builder;

  public static RecipeManager create(List<Field> constantFields) {
    return new RecipeManager(Collections.unmodifiableList(constantFields));
  }

  private RecipeManager(List<Field> constantFields) {
    this.constantFields = constantFields;
  }

  @Nullable
  public RequestRecipe previous() {
    synchronized (recipeLock) {
      return previousRecipe;
    }
  }

  @Nonnull
  public RecipeBuilder next() {
    synchronized (recipeLock) {
      if (builder == null) {
        builder = new RecipeBuilder(constantFields);
      }
      return builder;
    }
  }

  public final class RecipeBuilder {
    private final Set<Field> fields = new HashSet<>();

    @CanIgnoreReturnValue
    public RecipeBuilder addField(Field field) {
      fields.add(field);
      return this;
    }

    @CanIgnoreReturnValue
    public RecipeBuilder addAllFields(Collection<Field> fields) {
      this.fields.addAll(fields);
      return this;
    }

    @CanIgnoreReturnValue
    public RecipeBuilder merge(RequestRecipe recipe) {
      return addAllFields(recipe.getFields());
    }

    public RequestRecipe build() {
      synchronized (recipeLock) {
        RequestRecipe recipe = new RequestRecipe(Collections.unmodifiableCollection(fields));
        previousRecipe = recipe;
        builder = null;
        return recipe;
      }
    }

    private RecipeBuilder(List<Field> initialFields) {
      fields.addAll(initialFields);
    }
  }
}
