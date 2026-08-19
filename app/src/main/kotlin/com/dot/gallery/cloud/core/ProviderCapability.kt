/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.cloud.core

enum class ProviderCapability {
    PEOPLE,
    MAP,
    SMART_SEARCH,
    TEXT_SEARCH,
    /** Can create a public link for selected remote assets. */
    SHARE_CREATE,
    /** Can list, edit, and revoke previously created share links. */
    SHARE_MANAGE,
    SYNC,
    REMOTE_ALBUMS,
    REMOTE_ASSETS,
    OCR,
    ARCHIVE,
    MEMORIES,

    /** Server-side favorites that survive across devices (not local-only). */
    FAVORITE,

    /** A recoverable trash/bin: [RemoteMediaProvider.trashAsset] soft-deletes and
     * [RemoteMediaProvider.restoreAsset] can bring the item back. Providers that only
     * hard-delete (WebDAV/SMB/NFS, where trashAsset == deleteAsset) do NOT declare this. */
    TRASH
}
