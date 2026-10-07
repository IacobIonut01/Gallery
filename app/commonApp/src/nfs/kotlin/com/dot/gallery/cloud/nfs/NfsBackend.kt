/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.nfs

import com.dot.gallery.cloud.core.CloudServerConfig
import com.dot.gallery.cloud.core.ProviderType
import com.dot.gallery.cloud.netfs.FileSystemBackend
import com.dot.gallery.cloud.netfs.NetFsConnection
import com.dot.gallery.cloud.netfs.NetFsEntry
import com.dot.gallery.cloud.netfs.NetFsStorage
import com.emc.ecs.nfsclient.nfs.io.Nfs3File
import com.emc.ecs.nfsclient.nfs.io.NfsFileInputStream
import com.emc.ecs.nfsclient.nfs.io.NfsFileOutputStream
import com.emc.ecs.nfsclient.nfs.nfs3.Nfs3
import com.emc.ecs.nfsclient.rpc.CredentialUnix
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import com.dot.gallery.domain.uri.host
import com.dot.gallery.domain.uri.path


/**
 * NFSv3 backend powered by EMC nfs-client (pure-Java ONC RPC). Connection parameters are
 * encoded in the config's `serverUrl` as `nfs://host/exported/path`; the path after the
 * host is treated as the NFS export. `username` may carry `uid:gid` for AUTH_SYS (defaults
 * to 0:0). NFS AUTH_SYS is unauthenticated and unencrypted — see the integration plan.
 */
class NfsBackend : FileSystemBackend {

    override val providerType: ProviderType = ProviderType.NFS
    override val displayName: String = "NFS"

    private class NfsConnection(
        val nfs: Nfs3,
        val host: String,
        val export: String
    ) : NetFsConnection() {
        override val rootDisplay: String = "$host:$export"
    }

    override fun connect(config: CloudServerConfig): NetFsConnection {
        val raw = config.serverUrl.removePrefix("nfs://").removePrefix("NFS://").trimEnd('/')
        val host = raw.substringBefore('/')
        val export = "/" + raw.substringAfter('/', "").trimStart('/')
        require(host.isNotEmpty()) { "Invalid NFS URL: ${config.serverUrl}" }

        val (uid, gid) = parseUidGid(config.username)
        val nfs = Nfs3(host, export, CredentialUnix(uid, gid, null), 3)
        return NfsConnection(nfs, host, export)
    }

    override fun listDir(conn: NetFsConnection, path: String): List<NetFsEntry> {
        val c = conn as NfsConnection
        val dir = Nfs3File(c.nfs, nfsPath(path))
        if (!dir.exists() || !dir.isDirectory) return emptyList()
        return dir.listFiles().orEmpty()
            .filter { it.name != "." && it.name != ".." }
            .map { child ->
                val rel = if (path.isEmpty()) child.name else "$path/${child.name}"
                NetFsEntry(
                    name = child.name,
                    relativePath = rel,
                    isDirectory = runCatching { child.isDirectory }.getOrDefault(false),
                    size = runCatching { child.length() }.getOrDefault(0L),
                    lastModified = runCatching { child.lastModified() }.getOrDefault(0L)
                )
            }
    }

    override fun openRead(conn: NetFsConnection, path: String, offset: Long): InputStream {
        val c = conn as NfsConnection
        val file = Nfs3File(c.nfs, nfsPath(path))
        val stream: InputStream = NfsFileInputStream(file)
        if (offset > 0) {
            var remaining = offset
            while (remaining > 0) {
                val skipped = stream.skip(remaining)
                if (skipped <= 0) break
                remaining -= skipped
            }
        }
        return stream
    }

    override fun fileSize(conn: NetFsConnection, path: String): Long {
        val c = conn as NfsConnection
        return runCatching { Nfs3File(c.nfs, nfsPath(path)).length() }.getOrDefault(0L)
    }

    override fun exists(conn: NetFsConnection, path: String): Boolean {
        val c = conn as NfsConnection
        return runCatching { Nfs3File(c.nfs, nfsPath(path)).exists() }.getOrDefault(false)
    }

    override fun write(conn: NetFsConnection, path: String, data: InputStream, size: Long) {
        val c = conn as NfsConnection
        val segments = path.trim('/').split('/').filter(String::isNotEmpty)
        var parent = ""
        segments.dropLast(1).forEach { segment ->
            parent = if (parent.isEmpty()) segment else "$parent/$segment"
            val parentFile = Nfs3File(c.nfs, nfsPath(parent))
            if (!parentFile.exists()) {
                parentFile.mkdir()
                if (!Nfs3File(c.nfs, nfsPath(parent)).exists()) {
                    throw IllegalStateException("Could not create NFS directory $parent")
                }
            }
        }
        val file = Nfs3File(c.nfs, nfsPath(path))
        if (!file.exists() && !file.createNewFile()) {
            throw IllegalStateException("Could not create NFS file $path")
        }
        NfsFileOutputStream(Nfs3File(c.nfs, nfsPath(path))).use { data.copyTo(it) }
    }

    override fun delete(conn: NetFsConnection, path: String) {
        val c = conn as NfsConnection
        try {
            val file = Nfs3File(c.nfs, nfsPath(path))
            if (!file.exists()) return
            file.delete()
            if (file.exists()) throw IOException("NFS server kept deleted path $path")
        } catch (_: FileNotFoundException) {
            return
        }
    }

    override fun mkdir(conn: NetFsConnection, path: String) {
        val c = conn as NfsConnection
        val dir = Nfs3File(c.nfs, nfsPath(path))
        if (!dir.exists()) dir.mkdir()
    }

    override fun storage(conn: NetFsConnection): NetFsStorage? = null

    override fun close(conn: NetFsConnection) {
        // EMC nfs-client manages RPC connections internally; nothing to close explicitly.
    }

    private fun nfsPath(rel: String): String = if (rel.isEmpty()) "/" else "/" + rel.trimStart('/')

    private fun parseUidGid(username: String?): Pair<Int, Int> {
        val raw = username.orEmpty()
        if (!raw.contains(':')) return 0 to 0
        val uid = raw.substringBefore(':').toIntOrNull() ?: 0
        val gid = raw.substringAfter(':').toIntOrNull() ?: 0
        return uid to gid
    }
}
