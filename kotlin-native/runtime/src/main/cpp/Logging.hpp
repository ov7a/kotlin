/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

#ifndef RUNTIME_LOGGING_H
#define RUNTIME_LOGGING_H

#include <cstdarg>
#include <initializer_list>
#include <string_view>

#include "CompilerConstants.hpp"
#include "cpp_support/Span.hpp"
#include "Types.h"

namespace kotlin {
namespace logging {

enum class Level {
    kDebug,
    kInfo,
    kWarning,
    kError,
};

namespace internal {

class LogFilter {
public:
    virtual ~LogFilter() = default;

    virtual bool Empty() const noexcept = 0;
    virtual bool Enabled(Level level, std_support::span<const char* const> tags) const noexcept = 0;
};

KStdUniquePtr<LogFilter> CreateLogFilter(std::string_view tagsFilter) noexcept;

class Logger {
public:
    virtual ~Logger() = default;

    virtual void Log(Level level, std_support::span<const char* const> tags, std::string_view message) const noexcept = 0;
};

KStdUniquePtr<Logger> CreateStderrLogger() noexcept;

std_support::span<char> FormatLogEntry(
        std_support::span<char> buffer,
        Level level,
        std_support::span<const char* const> tags,
        const char* format,
        std::va_list args) noexcept;

void Log(
        const LogFilter& logFilter,
        const Logger& logger,
        Level level,
        std_support::span<const char* const> tags,
        const char* format,
        std::va_list args) noexcept;

} // namespace internal

__attribute__((format(printf, 3, 4))) void Log(Level level, std::initializer_list<const char*> tags, const char* format, ...) noexcept;
void VLog(Level level, std::initializer_list<const char*> tags, const char* format, std::va_list args) noexcept;

} // namespace logging
} // namespace kotlin

#endif // RUNTIME_LOGGING_H

// Using macros to simplify forwarding of varargs without breaking __attribute__((format)) and to avoid
// evaluating args in `...` if logging is disabled.

#define RuntimeLog(level, tags, format, ...) \
    do { \
        if (!::kotlin::compiler::enableRuntimeLogging().empty()) { \
            ::kotlin::logging::Log(level, tags, format, ##__VA_ARGS__); \
        } \
    } while (false)

#define RuntimeVLog(level, tags, format, args) \
    do { \
        if (!::kotlin::compiler::enableRuntimeLogging().empty()) { \
            ::kotlin::logging::VLog(level, tags, format, args); \
        } \
    } while (false)

#define RuntimeLogDebug(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kDebug, tags, format, ##__VA_ARGS__)
#define RuntimeLogInfo(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kInfo, tags, format, ##__VA_ARGS__)
#define RuntimeLogWarning(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kWarning, tags, format, ##__VA_ARGS__)
#define RuntimeLogError(tags, format, ...) RuntimeLog(::kotlin::logging::Level::kError, tags, format, ##__VA_ARGS__)

#define RuntimeVLogDebug(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kDebug, tags, format, args)
#define RuntimeVLogInfo(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kInfo, tags, format, args)
#define RuntimeVLogWarning(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kWarning, tags, format, args)
#define RuntimeVLogError(tags, format, args) RuntimeVLog(::kotlin::logging::Level::kError, tags, format, args)
