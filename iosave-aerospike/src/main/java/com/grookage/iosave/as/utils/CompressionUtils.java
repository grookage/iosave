/*
 * Copyright 2022 Koushik R <rkoushik.14@gmail.com>.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.grookage.iosave.as.utils;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import lombok.experimental.UtilityClass;

@UtilityClass
public class CompressionUtils {

  public static String compressAndEncode(byte[] bytes) throws IOException {
    final var bos = new ByteArrayOutputStream();
    final var gzip = new GZIPOutputStream(bos);
    gzip.write(bytes);
    gzip.close();

    return Base64.getEncoder().encodeToString(bos.toByteArray());
  }

  public static byte[] decodeAndDecompress(String value) throws IOException {
    final var gis = new GZIPInputStream(
        new ByteArrayInputStream(
            Base64.getDecoder().decode(value)
        ));
    final var bf = new BufferedReader(new InputStreamReader(gis));
    final var stringBuilder = new StringBuilder();
    String line;
    while ((line = bf.readLine()) != null) {
      stringBuilder.append(line);
    }
    return stringBuilder.toString().getBytes();
  }
}
