package com.dot.gallery.core.util

import com.dot.gallery.domain.uri.query





typealias Column = String

infix fun Query.or(other: Query) = Query(Node.Or(this.root, other.root))
infix fun Query.and(other: Query) = Query(Node.And(this.root, other.root))
infix fun Query.eq(other: Query) = Query(Node.Eq(this.root, other.root))
infix fun <T> Column.eq(other: T) = Query(Node.Literal(this)) eq Query(Node.Literal(other))

/**
 * Builds a `column LIKE '<pattern>'` clause. The [pattern] is emitted as a single-quoted SQL
 * literal (not a bound `?` argument), so it MUST only ever be called with trusted, compile-time
 * constant patterns (e.g. `"%.jxl"`) to avoid SQL injection.
 */
infix fun Column.like(pattern: String) =
    Query(Node.Like(Node.Literal(this), Node.Literal("'$pattern'")))

/**
 * Builds a `column IN (<v1>,<v2>,…)` clause. Values are emitted as inline literals (not bound
 * `?` arguments — query-bundle APIs cap argument count), so only numeric types are accepted;
 * they cannot carry SQL injection.
 */
infix fun Column.inValues(values: Collection<Long>) =
    Query(Node.In(Node.Literal(this), Node.Literal(values.joinToString(",", "(", ")"))))

fun Iterable<Query>.join(
    func: Query.(other: Query) -> Query,
) = reduceOrNull(func)