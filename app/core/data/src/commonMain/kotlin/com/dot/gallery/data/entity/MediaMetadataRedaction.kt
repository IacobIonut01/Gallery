/*
 * SPDX-FileCopyrightText: 2023-2026 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.data.entity



/**
 * The literal 0,0 coordinate pair is what MediaProvider writes into the GPS EXIF byte
 * ranges when it serves a file with location redacted (missing ACCESS_MEDIA_LOCATION)
 * — the IFD survives but parses as `GeoLocation(0.0, 0.0)`. Media genuinely lacking
 * GPS has no GeoLocation at all and stores NULL instead, so an exact 0,0 match is the
 * redaction signature and must be treated as "no coordinate", never as a position.
 */
fun isRedactedCoordinate(latitude: Double, longitude: Double): Boolean =
    latitude == 0.0 && longitude == 0.0
