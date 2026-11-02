/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.personalise;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.text.TextUtils;

import java.util.Locale;

/** Small helpers shared by the Keybox and PIF file pickers. */
final class PersonaliseFileUtils {

    private PersonaliseFileUtils() {}

    /**
     * Returns whether the document behind {@code uri} looks like a file of the wanted kind, either
     * because its reported MIME type is one of {@code mimeTypes} or because its display name ends
     * with {@code extension}. Content URIs from some providers (e.g. Downloads) do not carry the
     * file name, so checking the URI string alone rejects valid files.
     */
    static boolean matches(Context context, Uri uri, String extension, String... mimeTypes) {
        if (uri == null) {
            return false;
        }
        final String type = context.getContentResolver().getType(uri);
        if (type != null) {
            for (String mime : mimeTypes) {
                if (mime.equalsIgnoreCase(type)) {
                    return true;
                }
            }
        }
        final String suffix = extension.toLowerCase(Locale.ROOT);
        final String name = getDisplayName(context, uri);
        if (!TextUtils.isEmpty(name) && name.toLowerCase(Locale.ROOT).endsWith(suffix)) {
            return true;
        }
        final String path = uri.toString();
        return path.toLowerCase(Locale.ROOT).endsWith(suffix);
    }

    private static String getDisplayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(uri,
                new String[] {OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (RuntimeException e) {
            // Provider does not expose a display name; fall back to the URI.
        }
        return null;
    }
}
