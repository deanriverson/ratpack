/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ratpack.file.internal;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import ratpack.file.BaseDirRequiredException;
import ratpack.file.FileSystemBinding;
import ratpack.handling.Context;
import ratpack.handling.Handler;
import ratpack.server.ServerConfig;
import ratpack.registry.Registry;

import java.util.concurrent.ExecutionException;

public class FileSystemBindingHandler implements Handler {

  private final String path;
  private final Handler handler;

  private final static LoadingCache<FileSystemBinding, Registry> CACHE = CacheBuilder.newBuilder()
    .maximumSize(1024)
    .build(new CacheLoader<FileSystemBinding, Registry>() {
      @Override
      public Registry load(FileSystemBinding key) throws Exception {
        return Registry.single(FileSystemBinding.class, key);
      }
    });

  public FileSystemBindingHandler(ServerConfig serverConfig, String path, Handler handler) {
    if (serverConfig.isHasBaseDir()) {
      this.path = path;
      this.handler = handler;
    } else {
      throw new BaseDirRequiredException("An application base directory is required to use this handler");
    }

    // TODO - validate the path isn't escaping up with ../
  }

  public void handle(Context context) throws ExecutionException {
    FileSystemBinding parentBinding = context.get(FileSystemBinding.class);
    FileSystemBinding binding = parentBinding.binding(path);
    if (binding == null) {
      context.clientError(404);
    } else {
      context.insert(CACHE.get(binding), handler);
    }
  }
}
