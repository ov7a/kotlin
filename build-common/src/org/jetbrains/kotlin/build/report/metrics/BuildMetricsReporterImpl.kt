/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.report.metrics

import java.util.*

class BuildMetricsReporterImpl : BuildMetricsReporter {
    private val myBuildTimeStartNs: EnumMap<BuildTime, Long> =
        EnumMap(
            BuildTime::class.java
        )
    private val myBuildTimes = BuildTimes()
    private val myBuildAttributes = BuildAttributes()

    override fun startMeasure(time: BuildTime, startNs: Long) {
        if (time in myBuildTimeStartNs) {
            error("$time was restarted before it finished")
        }
        myBuildTimeStartNs[time] = startNs
    }

    override fun endMeasure(time: BuildTime, endNs: Long) {
        val startNs = myBuildTimeStartNs.remove(time) ?: error("$time finished before it started")
        val durationNs = endNs - startNs
        myBuildTimes.add(time, durationNs)
    }

    override fun addMetric(metric: BuildTime, value: Long) {
        myBuildTimes.add(metric, value)
    }

    override fun addAttribute(attribute: BuildAttribute) {
        myBuildAttributes.add(attribute)
    }

    override fun getMetrics(): BuildMetrics =
        BuildMetrics(
            buildTimes = myBuildTimes,
            buildAttributes = myBuildAttributes
        )

    override fun addMetrics(metrics: BuildMetrics?) {
        if (metrics == null) return

        myBuildAttributes.addAll(metrics.buildAttributes)
        myBuildTimes.addAll(metrics.buildTimes)
    }
}