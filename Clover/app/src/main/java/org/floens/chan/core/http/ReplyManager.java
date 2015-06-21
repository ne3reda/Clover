/*
 * Clover - 4chan browser https://github.com/Floens/Clover/
 * Copyright (C) 2014  Floens
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.floens.chan.core.http;

import android.content.Context;
import android.content.Intent;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;

import org.floens.chan.Chan;
import org.floens.chan.core.model.Loadable;
import org.floens.chan.core.model.Reply;
import org.floens.chan.ui.activity.ImagePickActivity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * To send an reply to 4chan.
 */
public class ReplyManager {
    private static final int TIMEOUT = 30000;

    private final Context context;
    private FileListener fileListener;
    private OkHttpClient client;

    private Map<Loadable, Reply> drafts = new HashMap<>();

    public ReplyManager(Context context) {
        this.context = context;

        client = new OkHttpClient();
        client.setConnectTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        client.setReadTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
        client.setWriteTimeout(TIMEOUT, TimeUnit.MILLISECONDS);
    }

    public Reply getReply(Loadable loadable) {
        Reply reply = drafts.get(loadable);
        if (reply == null) {
            reply = new Reply();
            drafts.put(loadable, reply);
        }
        return reply;
    }

    public void putReply(Loadable loadable, Reply reply) {
        // Remove files from all other replies because there can only be one picked_file at the same time.
        // Not doing this would be confusing and cause invalid fileNames.
        for (Map.Entry<Loadable, Reply> entry : drafts.entrySet()) {
            if (!entry.getKey().equals(loadable)) {
                Reply value = entry.getValue();
                value.file = null;
                value.fileName = "";
            }
        }

        drafts.put(loadable, reply);
    }

    /**
     * Pick an file. Starts up the ImagePickActivity.
     *
     * @param listener FileListener to listen on.
     */
    public void pickFile(FileListener listener) {
        fileListener = listener;

        Intent intent = new Intent(context, ImagePickActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public File getPickFile() {
        return new File(context.getCacheDir(), "picked_file");
    }

    public void _onFilePickLoading() {
        if (fileListener != null) {
            fileListener.onFilePickLoading();
        }
    }

    public void _onFilePicked(String name, File file) {
        if (fileListener != null) {
            fileListener.onFilePicked(name, file);
            fileListener = null;
        }
    }

    public void _onFilePickError(boolean cancelled) {
        if (fileListener != null) {
            fileListener.onFilePickError(cancelled);
            fileListener = null;
        }
    }

    public interface FileListener {
        void onFilePickLoading();

        void onFilePicked(String name, File file);

        void onFilePickError(boolean cancelled);
    }

    public void makeHttpCall(HttpCall httpCall, HttpCallback callback) {
        //noinspection unchecked
        httpCall.setCallback(callback);

        Request.Builder requestBuilder = new Request.Builder();

        httpCall.setup(requestBuilder);

        requestBuilder.header("User-Agent", Chan.getInstance().getUserAgent());
        Request request = requestBuilder.build();

        client.newCall(request).enqueue(httpCall);
    }

    public interface HttpCallback<T extends HttpCall> {
        void onHttpSuccess(T httpPost);

        void onHttpFail(T httpPost);
    }
}
