/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.core.logging


/**
 * Severity levels for [LogEntry]. [EVENT] entries are breadcrumbs (user actions,
 * navigation) rather than diagnostic messages; [CRASH] marks an uncaught
 * exception captured by `CrashCapture`.
 */
enum class LogLevel { VERBOSE, DEBUG, INFO, WARN, ERROR, CRASH, EVENT }
