/*
 * Copyright (C) 2018 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.common.hash;

import com.google.common.base.Supplier;
import com.google.errorprone.annotations.Immutable;
import org.jspecify.nullness.Nullable;

/**
 * Explicitly named subinterface of {@link Supplier} that can be marked {@literal @}{@link
 * Immutable}.
 */
@Immutable
interface ImmutableSupplier<T extends @Nullable Object> extends Supplier<T> {}
