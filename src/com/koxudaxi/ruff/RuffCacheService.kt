package com.koxudaxi.ruff

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
class RuffCacheService(val project: Project) {
    private var version: RuffVersion? = null
    private var hasFormatter: Boolean? = null
    private var hasOutputFormat: Boolean? = null


    fun getVersion(): RuffVersion? {
        val v = executeOnPooledThread(null) {
            runRuff(project, listOf("--version"), true)
        }
        return v?.let { getOrPutVersionFromVersionCache(it) }
    }

    private fun getOrPutVersionFromVersionCache(version: String): RuffVersion? {
        return ruffVersionCache.getOrPut(version) {
            val versionList =
                version.trimEnd().split(" ").lastOrNull()?.split(".")?.map { it.toIntOrNull() ?: 0 } ?: return null
            val ruffVersion = when {
                versionList.size == 1 -> RuffVersion(versionList[0])
                versionList.size == 2 -> RuffVersion(versionList[0], versionList[1])
                versionList.size >= 3 -> RuffVersion(versionList[0], versionList[1], versionList[2])
                else -> null
            } ?: return null
            ruffVersionCache[version] = ruffVersion
            ruffVersion
        }
    }

    internal fun getOrPutVersion(): RuffVersion? {
        if (version != null) return version
        return getVersion().apply {
            version = this
            hasFormatter = this?.hasFormatter
            hasOutputFormat = this?.hasOutputFormat
        }
    }

    internal fun setVersion(): RuffVersion? {
        return getVersion().also {
            this.version = it
            hasFormatter = it?.hasFormatter
            hasOutputFormat = it?.hasOutputFormat
        }
    }

    internal fun hasFormatter(): Boolean {
        if (hasFormatter == null) {
            setVersion()
        }
        return hasFormatter ?: false
    }

    internal fun hasOutputFormat(): Boolean {
        if (hasOutputFormat == null) {
            setVersion()
        }
        return hasOutputFormat ?: false
    }

    internal
    companion object {
        fun hasFormatter(project: Project): Boolean = getInstance(project).hasFormatter()

        fun hasOutputFormat(project: Project): Boolean = getInstance(project).hasOutputFormat()

        fun getVersion(project: Project): RuffVersion? {
            return getInstance(project).getOrPutVersion()
        }

        fun setVersion(project: Project): RuffVersion? {
            return getInstance(project).setVersion()
        }

        fun getOrPutVersionFromVersionCache(project: Project, version: String): RuffVersion? {
            return getInstance(project).getOrPutVersionFromVersionCache(version)
        }

        internal fun getInstance(project: Project): RuffCacheService {
            return project.getService(RuffCacheService::class.java)
        }
    }
}