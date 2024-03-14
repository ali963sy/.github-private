/*
 * Copyright (C) 2008 The Guava Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.common.collect;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.NullnessCasts.uncheckedCastNullableTToT;

import com.google.common.annotations.Beta;
import com.google.common.annotations.GwtCompatible;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Supplier;
import com.google.common.collect.Table.Cell;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import javax.annotation.CheckForNull;
import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides static methods that involve a {@code Table}.
 *
 * <p>See the Guava User Guide article on <a href=
 * "https://github.com/google/guava/wiki/CollectionUtilitiesExplained#tables">{@code Tables}</a>.
 *
 * @author Jared Levy
 * @author Louis Wasserman
 * @since 7.0
 */
@GwtCompatible
@ElementTypesAreNonnullByDefault
public final class Tables {
  private Tables() {}

  /**
   * Returns an immutable cell with the specified row key, column key, and value.
   *
   * <p>The returned cell is serializable.
   *
   * @param rowKey the row key to be associated with the returned cell
   * @param columnKey the column key to be associated with the returned cell
   * @param value the value to be associated with the returned cell
   */
  public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      Cell<R, C, V> immutableCell(
          @ParametricNullness R rowKey,
          @ParametricNullness C columnKey,
          @ParametricNullness V value) {
    return new ImmutableCell<>(rowKey, columnKey, value);
  }

  static final class ImmutableCell<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      extends AbstractCell<R, C, V> implements Serializable {
    @ParametricNullness private final R rowKey;
    @ParametricNullness private final C columnKey;
    @ParametricNullness private final V value;

    ImmutableCell(
        @ParametricNullness R rowKey,
        @ParametricNullness C columnKey,
        @ParametricNullness V value) {
      this.rowKey = rowKey;
      this.columnKey = columnKey;
      this.value = value;
    }

    @Override
    @ParametricNullness
    public R getRowKey() {
      return rowKey;
    }

    @Override
    @ParametricNullness
    public C getColumnKey() {
      return columnKey;
    }

    @Override
    @ParametricNullness
    public V getValue() {
      return value;
    }

    private static final long serialVersionUID = 0;
  }

  abstract static class AbstractCell<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      implements Cell<R, C, V> {
    // needed for serialization
    AbstractCell() {}

    @Override
    public boolean equals(@CheckForNull Object obj) {
      if (obj == this) {
        return true;
      }
      if (obj instanceof Cell) {
        Cell<?, ?, ?> other = (Cell<?, ?, ?>) obj;
        return Objects.equal(getRowKey(), other.getRowKey())
            && Objects.equal(getColumnKey(), other.getColumnKey())
            && Objects.equal(getValue(), other.getValue());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(getRowKey(), getColumnKey(), getValue());
    }

    @Override
    public String toString() {
      return "(" + getRowKey() + "," + getColumnKey() + ")=" + getValue();
    }
  }

  /**
   * Creates a transposed view of a given table that flips its row and column keys. In other words,
   * calling {@code get(columnKey, rowKey)} on the generated table always returns the same value as
   * calling {@code get(rowKey, columnKey)} on the original table. Updating the original table
   * changes the contents of the transposed table and vice versa.
   *
   * <p>The returned table supports update operations as long as the input table supports the
   * analogous operation with swapped rows and columns. For example, in a {@link HashBasedTable}
   * instance, {@code rowKeySet().iterator()} supports {@code remove()} but {@code
   * columnKeySet().iterator()} doesn't. With a transposed {@link HashBasedTable}, it's the other
   * way around.
   */
  public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      Table<C, R, V> transpose(Table<R, C, V> table) {
    return (table instanceof TransposeTable)
        ? ((TransposeTable<R, C, V>) table).original
        : new TransposeTable<C, R, V>(table);
  }

  private static class TransposeTable<
          C extends @Nullable Object, R extends @Nullable Object, V extends @Nullable Object>
      extends AbstractTable<C, R, V> {
    final Table<R, C, V> original;

    TransposeTable(Table<R, C, V> original) {
      this.original = checkNotNull(original);
    }

    @Override
    public void clear() {
      original.clear();
    }

    @Override
    public Map<C, V> column(@ParametricNullness R columnKey) {
      return original.row(columnKey);
    }

    @Override
    public Set<R> columnKeySet() {
      return original.rowKeySet();
    }

    @Override
    public Map<R, Map<C, V>> columnMap() {
      return original.rowMap();
    }

    @Override
    public boolean contains(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      return original.contains(columnKey, rowKey);
    }

    @Override
    public boolean containsColumn(@CheckForNull Object columnKey) {
      return original.containsRow(columnKey);
    }

    @Override
    public boolean containsRow(@CheckForNull Object rowKey) {
      return original.containsColumn(rowKey);
    }

    @Override
    public boolean containsValue(@CheckForNull Object value) {
      return original.containsValue(value);
    }

    @Override
    @CheckForNull
    public V get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      return original.get(columnKey, rowKey);
    }

    @Override
    @CheckForNull
    public V put(
        @ParametricNullness C rowKey,
        @ParametricNullness R columnKey,
        @ParametricNullness V value) {
      return original.put(columnKey, rowKey, value);
    }

    @Override
    public void putAll(Table<? extends C, ? extends R, ? extends V> table) {
      original.putAll(transpose(table));
    }

    @Override
    @CheckForNull
    public V remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      return original.remove(columnKey, rowKey);
    }

    @Override
    public Map<R, V> row(@ParametricNullness C rowKey) {
      return original.column(rowKey);
    }

    @Override
    public Set<C> rowKeySet() {
      return original.columnKeySet();
    }

    @Override
    public Map<C, Map<R, V>> rowMap() {
      return original.columnMap();
    }

    @Override
    public int size() {
      return original.size();
    }

    @Override
    public Collection<V> values() {
      return original.values();
    }

    // Will cast TRANSPOSE_CELL to a type that always succeeds
    private static final Function<Cell<?, ?, ?>, Cell<?, ?, ?>> TRANSPOSE_CELL =
        new Function<Cell<?, ?, ?>, Cell<?, ?, ?>>() {
          @Override
          public Cell<?, ?, ?> apply(Cell<?, ?, ?> cell) {
            return immutableCell(cell.getColumnKey(), cell.getRowKey(), cell.getValue());
          }
        };

    @SuppressWarnings("unchecked")
    @Override
    Iterator<Cell<C, R, V>> cellIterator() {
      return Iterators.transform(original.cellSet().iterator(), (Function) TRANSPOSE_CELL);
    }
  }

  /**
   * Creates a table that uses the specified backing map and factory. It can generate a table based
   * on arbitrary {@link Map} classes.
   *
   * <p>The {@code factory}-generated and {@code backingMap} classes determine the table iteration
   * order. However, the table's {@code row()} method returns instances of a different class than
   * {@code factory.get()} does.
   *
   * <p>Call this method only when the simpler factory methods in classes like {@link
   * HashBasedTable} and {@link TreeBasedTable} won't suffice.
   *
   * <p>The views returned by the {@code Table} methods {@link Table#column}, {@link
   * Table#columnKeySet}, and {@link Table#columnMap} have iterators that don't support {@code
   * remove()}. Otherwise, all optional operations are supported. Null row keys, columns keys, and
   * values are not supported.
   *
   * <p>Lookups by row key are often faster than lookups by column key, because the data is stored
   * in a {@code Map<R, Map<C, V>>}. A method call like {@code column(columnKey).get(rowKey)} still
   * runs quickly, since the row key is provided. However, {@code column(columnKey).size()} takes
   * longer, since an iteration across all row keys occurs.
   *
   * <p>Note that this implementation is not synchronized. If multiple threads access this table
   * concurrently and one of the threads modifies the table, it must be synchronized externally.
   *
   * <p>The table is serializable if {@code backingMap}, {@code factory}, the maps generated by
   * {@code factory}, and the table contents are all serializable.
   *
   * <p>Note: the table assumes complete ownership over of {@code backingMap} and the maps returned
   * by {@code factory}. Those objects should not be manually updated and they should not use soft,
   * weak, or phantom references.
   *
   * @param backingMap place to store the mapping from each row key to its corresponding column key
   *     / value map
   * @param factory supplier of new, empty maps that will each hold all column key / value mappings
   *     for a given row key
   * @throws IllegalArgumentException if {@code backingMap} is not empty
   * @since 10.0
   */
  @Beta
  public static <R, C, V> Table<R, C, V> newCustomTable(
      Map<R, Map<C, V>> backingMap, Supplier<? extends Map<C, V>> factory) {
    checkArgument(backingMap.isEmpty());
    checkNotNull(factory);
    // TODO(jlevy): Wrap factory to validate that the supplied maps are empty?
    return new StandardTable<>(backingMap, factory);
  }

  /**
   * Returns a view of a table where each value is transformed by a function. All other properties
   * of the table, such as iteration order, are left intact.
   *
   * <p>Changes in the underlying table are reflected in this view. Conversely, this view supports
   * removal operations, and these are reflected in the underlying table.
   *
   * <p>It's acceptable for the underlying table to contain null keys, and even null values provided
   * that the function is capable of accepting null input. The transformed table might contain null
   * values, if the function sometimes gives a null result.
   *
   * <p>The returned table is not thread-safe or serializable, even if the underlying table is.
   *
   * <p>The function is applied lazily, invoked when needed. This is necessary for the returned
   * table to be a view, but it means that the function will be applied many times for bulk
   * operations like {@link Table#containsValue} and {@code Table.toString()}. For this to perform
   * well, {@code function} should be fast. To avoid lazy evaluation when the returned table doesn't
   * need to be a view, copy the returned table into a new table of your choosing.
   *
   * @since 10.0
   */
  @Beta
  public static <
          R extends @Nullable Object,
          C extends @Nullable Object,
          V1 extends @Nullable Object,
          V2 extends @Nullable Object>
      Table<R, C, V2> transformValues(
          Table<R, C, V1> fromTable, Function<? super V1, V2> function) {
    return new TransformedTable<>(fromTable, function);
  }

  private static class TransformedTable<
          R extends @Nullable Object,
          C extends @Nullable Object,
          V1 extends @Nullable Object,
          V2 extends @Nullable Object>
      extends AbstractTable<R, C, V2> {
    final Table<R, C, V1> fromTable;
    final Function<? super V1, V2> function;

    TransformedTable(Table<R, C, V1> fromTable, Function<? super V1, V2> function) {
      this.fromTable = checkNotNull(fromTable);
      this.function = checkNotNull(function);
    }

    @Override
    public boolean contains(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      return fromTable.contains(rowKey, columnKey);
    }

    @Override
    @CheckForNull
    public V2 get(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      // The function is passed a null input only when the table contains a null
      // value.
      // The cast is safe because of the contains() check.
      return contains(rowKey, columnKey)
          ? function.apply(uncheckedCastNullableTToT(fromTable.get(rowKey, columnKey)))
          : null;
    }

    @Override
    public int size() {
      return fromTable.size();
    }

    @Override
    public void clear() {
      fromTable.clear();
    }

    @Override
    @CheckForNull
    public V2 put(
        @ParametricNullness R rowKey,
        @ParametricNullness C columnKey,
        @ParametricNullness V2 value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V2> table) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public V2 remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      return contains(rowKey, columnKey)
          // The cast is safe because of the contains() check.
          ? function.apply(uncheckedCastNullableTToT(fromTable.remove(rowKey, columnKey)))
          : null;
    }

    @Override
    public Map<C, V2> row(@ParametricNullness R rowKey) {
      return Maps.transformValues(fromTable.row(rowKey), function);
    }

    @Override
    public Map<R, V2> column(@ParametricNullness C columnKey) {
      return Maps.transformValues(fromTable.column(columnKey), function);
    }

    Function<Cell<R, C, V1>, Cell<R, C, V2>> cellFunction() {
      return new Function<Cell<R, C, V1>, Cell<R, C, V2>>() {
        @Override
        public Cell<R, C, V2> apply(Cell<R, C, V1> cell) {
          return immutableCell(
              cell.getRowKey(), cell.getColumnKey(), function.apply(cell.getValue()));
        }
      };
    }

    @Override
    Iterator<Cell<R, C, V2>> cellIterator() {
      return Iterators.transform(fromTable.cellSet().iterator(), cellFunction());
    }

    @Override
    public Set<R> rowKeySet() {
      return fromTable.rowKeySet();
    }

    @Override
    public Set<C> columnKeySet() {
      return fromTable.columnKeySet();
    }

    @Override
    Collection<V2> createValues() {
      return Collections2.transform(fromTable.values(), function);
    }

    @Override
    public Map<R, Map<C, V2>> rowMap() {
      Function<Map<C, V1>, Map<C, V2>> rowFunction =
          new Function<Map<C, V1>, Map<C, V2>>() {
            @Override
            public Map<C, V2> apply(Map<C, V1> row) {
              return Maps.transformValues(row, function);
            }
          };
      return Maps.transformValues(fromTable.rowMap(), rowFunction);
    }

    @Override
    public Map<C, Map<R, V2>> columnMap() {
      Function<Map<R, V1>, Map<R, V2>> columnFunction =
          new Function<Map<R, V1>, Map<R, V2>>() {
            @Override
            public Map<R, V2> apply(Map<R, V1> column) {
              return Maps.transformValues(column, function);
            }
          };
      return Maps.transformValues(fromTable.columnMap(), columnFunction);
    }
  }

  /**
   * Returns an unmodifiable view of the specified table. This method allows modules to provide
   * users with "read-only" access to internal tables. Query operations on the returned table "read
   * through" to the specified table, and attempts to modify the returned table, whether direct or
   * via its collection views, result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned table will be serializable if the specified table is serializable.
   *
   * <p>Consider using an {@link ImmutableTable}, which is guaranteed never to change.
   *
   * @since 11.0
   */
  public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      Table<R, C, V> unmodifiableTable(Table<? extends R, ? extends C, ? extends V> table) {
    return new UnmodifiableTable<>(table);
  }

  private static class UnmodifiableTable<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      extends ForwardingTable<R, C, V> implements Serializable {
    final Table<? extends R, ? extends C, ? extends V> delegate;

    UnmodifiableTable(Table<? extends R, ? extends C, ? extends V> delegate) {
      this.delegate = checkNotNull(delegate);
    }

    @SuppressWarnings("unchecked") // safe, covariant cast
    @Override
    protected Table<R, C, V> delegate() {
      return (Table<R, C, V>) delegate;
    }

    @Override
    public Set<Cell<R, C, V>> cellSet() {
      return Collections.unmodifiableSet(super.cellSet());
    }

    @Override
    public void clear() {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<R, V> column(@ParametricNullness C columnKey) {
      return Collections.unmodifiableMap(super.column(columnKey));
    }

    @Override
    public Set<C> columnKeySet() {
      return Collections.unmodifiableSet(super.columnKeySet());
    }

    @Override
    public Map<C, Map<R, V>> columnMap() {
      Function<Map<R, V>, Map<R, V>> wrapper = unmodifiableWrapper();
      return Collections.unmodifiableMap(Maps.transformValues(super.columnMap(), wrapper));
    }

    @Override
    @CheckForNull
    public V put(
        @ParametricNullness R rowKey,
        @ParametricNullness C columnKey,
        @ParametricNullness V value) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Table<? extends R, ? extends C, ? extends V> table) {
      throw new UnsupportedOperationException();
    }

    @Override
    @CheckForNull
    public V remove(@CheckForNull Object rowKey, @CheckForNull Object columnKey) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Map<C, V> row(@ParametricNullness R rowKey) {
      return Collections.unmodifiableMap(super.row(rowKey));
    }

    @Override
    public Set<R> rowKeySet() {
      return Collections.unmodifiableSet(super.rowKeySet());
    }

    @Override
    public Map<R, Map<C, V>> rowMap() {
      Function<Map<C, V>, Map<C, V>> wrapper = unmodifiableWrapper();
      return Collections.unmodifiableMap(Maps.transformValues(super.rowMap(), wrapper));
    }

    @Override
    public Collection<V> values() {
      return Collections.unmodifiableCollection(super.values());
    }

    private static final long serialVersionUID = 0;
  }

  /**
   * Returns an unmodifiable view of the specified row-sorted table. This method allows modules to
   * provide users with "read-only" access to internal tables. Query operations on the returned
   * table "read through" to the specified table, and attempts to modify the returned table, whether
   * direct or via its collection views, result in an {@code UnsupportedOperationException}.
   *
   * <p>The returned table will be serializable if the specified table is serializable.
   *
   * @param table the row-sorted table for which an unmodifiable view is to be returned
   * @return an unmodifiable view of the specified table
   * @since 11.0
   */
  @Beta
  public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      RowSortedTable<R, C, V> unmodifiableRowSortedTable(
          RowSortedTable<R, ? extends C, ? extends V> table) {
    /*
     * It's not ? extends R, because it's technically not covariant in R. Specifically,
     * table.rowMap().comparator() could return a comparator that only works for the ? extends R.
     * Collections.unmodifiableSortedMap makes the same distinction.
     */
    return new UnmodifiableRowSortedMap<>(table);
  }

  static final class UnmodifiableRowSortedMap<
          R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      extends UnmodifiableTable<R, C, V> implements RowSortedTable<R, C, V> {

    public UnmodifiableRowSortedMap(RowSortedTable<R, ? extends C, ? extends V> delegate) {
      super(delegate);
    }

    @Override
    protected RowSortedTable<R, C, V> delegate() {
      return (RowSortedTable<R, C, V>) super.delegate();
    }

    @Override
    public SortedMap<R, Map<C, V>> rowMap() {
      Function<Map<C, V>, Map<C, V>> wrapper = unmodifiableWrapper();
      return Collections.unmodifiableSortedMap(Maps.transformValues(delegate().rowMap(), wrapper));
    }

    @Override
    public SortedSet<R> rowKeySet() {
      return Collections.unmodifiableSortedSet(delegate().rowKeySet());
    }

    private static final long serialVersionUID = 0;
  }

  @SuppressWarnings("unchecked")
  private static <K extends @Nullable Object, V extends @Nullable Object>
      Function<Map<K, V>, Map<K, V>> unmodifiableWrapper() {
    return (Function) UNMODIFIABLE_WRAPPER;
  }

  private static final Function<? extends Map<?, ?>, ? extends Map<?, ?>> UNMODIFIABLE_WRAPPER =
      new Function<Map<Object, Object>, Map<Object, Object>>() {
        @Override
        public Map<Object, Object> apply(Map<Object, Object> input) {
          return Collections.unmodifiableMap(input);
        }
      };

  /**
   * Returns a synchronized (thread-safe) table backed by the specified table. In order to guarantee
   * serial access, it is critical that <b>all</b> access to the backing table is accomplished
   * through the returned table.
   *
   * <p>It is imperative that the user manually synchronize on the returned table when accessing any
   * of its collection views:
   *
   * <pre>{@code
   * Table<R, C, V> table = Tables.synchronizedTable(HashBasedTable.<R, C, V>create());
   * ...
   * Map<C, V> row = table.row(rowKey);  // Needn't be in synchronized block
   * ...
   * synchronized (table) {  // Synchronizing on table, not row!
   *   Iterator<Entry<C, V>> i = row.entrySet().iterator(); // Must be in synchronized block
   *   while (i.hasNext()) {
   *     foo(i.next());
   *   }
   * }
   * }</pre>
   *
   * <p>Failure to follow this advice may result in non-deterministic behavior.
   *
   * <p>The returned table will be serializable if the specified table is serializable.
   *
   * @param table the table to be wrapped in a synchronized view
   * @return a synchronized view of the specified table
   * @since 22.0
   */
  public static <R extends @Nullable Object, C extends @Nullable Object, V extends @Nullable Object>
      Table<R, C, V> synchronizedTable(Table<R, C, V> table) {
    return Synchronized.table(table, null);
  }

  static boolean equalsImpl(Table<?, ?, ?> table, @CheckForNull Object obj) {
    if (obj == table) {
      return true;
    } else if (obj instanceof Table) {
      Table<?, ?, ?> that = (Table<?, ?, ?>) obj;
      return table.cellSet().equals(that.cellSet());
    } else {
      return false;
    }
  }
}