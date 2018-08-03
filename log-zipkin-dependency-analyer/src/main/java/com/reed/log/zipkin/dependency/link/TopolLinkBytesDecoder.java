/*
 * Copyright 2015-2018 The OpenZipkin Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.reed.log.zipkin.dependency.link;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import zipkin2.codec.BytesDecoder;
import zipkin2.codec.Encoding;
import zipkin2.internal.JsonCodec;
import zipkin2.internal.JsonCodec.JsonReader;
import zipkin2.internal.Nullable;

public enum TopolLinkBytesDecoder implements BytesDecoder<TopolLink> {
	JSON_V1 {
		@Override
		public Encoding encoding() {
			return Encoding.JSON;
		}

		@Override
		public boolean decode(byte[] link, Collection<TopolLink> out) {
			return JsonCodec.read(READER, link, out);
		}

		@Override
		@Nullable
		public TopolLink decodeOne(byte[] link) {
			return JsonCodec.readOne(READER, link);
		}

		@Override
		public boolean decodeList(byte[] links, Collection<TopolLink> out) {
			return JsonCodec.readList(READER, links, out);
		}

		@Override
		public List<TopolLink> decodeList(byte[] links) {
			List<TopolLink> out = new ArrayList<>();
			if (!decodeList(links, out))
				return Collections.emptyList();
			return out;
		}
	};

	static final JsonCodec.JsonReaderAdapter<TopolLink> READER = new JsonCodec.JsonReaderAdapter<TopolLink>() {
		@Override
		public TopolLink fromJson(JsonReader reader) throws IOException {
			TopolLink.Builder result = TopolLink.newBuilder();
			reader.beginObject();
			while (reader.hasNext()) {
				String nextName = reader.nextName();
				if (nextName.equals("parent")) {
					result.parent(reader.nextString());
				} else if (nextName.equals("child")) {
					result.child(reader.nextString());
				} else if (nextName.equals("callCount")) {
					result.callCount(reader.nextLong());
				} else if (nextName.equals("errorCount")) {
					result.errorCount(reader.nextLong());
				} else if (nextName.equals("qps")) {
					result.qps(Double.valueOf(reader.nextString()));
				} else if (nextName.equals("cost")) {
					result.cost(Double.valueOf(reader.nextString()));
				} else if (nextName.equals("timestamp")) {
					result.timestamp(reader.nextLong());
				} else {
					reader.skipValue();
				}
			}
			reader.endObject();
			return result.build();
		}

		@Override
		public String toString() {
			return "TopolLink";
		}
	};
}
