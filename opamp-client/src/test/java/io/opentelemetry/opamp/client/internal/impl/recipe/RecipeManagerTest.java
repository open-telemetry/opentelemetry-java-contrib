/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.opamp.client.internal.impl.recipe;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.opamp.client.internal.request.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import org.junit.jupiter.api.Test;

class RecipeManagerTest {

  @Test
  void verifyConstantValues() {
    RecipeManager recipeManager =
        RecipeManager.create(getFieldsAsList(Field.AGENT_DESCRIPTION, Field.FLAGS));

    // First run
    assertThat(recipeManager.next().build().getFields())
        .containsExactlyInAnyOrder(Field.AGENT_DESCRIPTION, Field.FLAGS);

    // Adding extra fields
    recipeManager.next().addField(Field.CAPABILITIES);

    assertThat(recipeManager.next().build().getFields())
        .containsExactlyInAnyOrder(Field.AGENT_DESCRIPTION, Field.FLAGS, Field.CAPABILITIES);

    // Not adding fields for the next build
    assertThat(recipeManager.next().build().getFields())
        .containsExactlyInAnyOrder(Field.AGENT_DESCRIPTION, Field.FLAGS);
  }

  @Test
  void verifyPreviousFields() {
    RecipeManager recipeManager =
        RecipeManager.create(getFieldsAsList(Field.CAPABILITIES, Field.FLAGS));

    // Previous build when there's none
    assertThat(recipeManager.previous()).isNull();

    // First build
    Collection<Field> fields =
        recipeManager.next().addField(Field.REMOTE_CONFIG_STATUS).build().getFields();
    assertThat(fields)
        .containsExactlyInAnyOrder(Field.CAPABILITIES, Field.FLAGS, Field.REMOTE_CONFIG_STATUS);
    assertThat(recipeManager.previous().getFields()).isEqualTo(fields);

    // Merging fields
    recipeManager.next().addField(Field.AGENT_DISCONNECT).merge(recipeManager.previous());
    assertThat(recipeManager.next().build().getFields())
        .containsExactlyInAnyOrder(
            Field.CAPABILITIES, Field.FLAGS, Field.REMOTE_CONFIG_STATUS, Field.AGENT_DISCONNECT);
  }

  @Nonnull
  private static List<Field> getFieldsAsList(Field... fields) {
    return new ArrayList<>(Arrays.asList(fields));
  }
}
