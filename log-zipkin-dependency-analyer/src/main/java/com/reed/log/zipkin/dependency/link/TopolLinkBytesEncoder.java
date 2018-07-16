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

import static zipkin2.internal.Buffer.asciiSizeInBytes;
import static zipkin2.internal.JsonEscaper.jsonEscape;
import static zipkin2.internal.JsonEscaper.jsonEscapedSizeInBytes;

import java.util.List;

import zipkin2.codec.BytesEncoder;
import zipkin2.codec.Encoding;
import zipkin2.internal.Buffer;
import zipkin2.internal.JsonCodec;

public enum TopolLinkBytesEncoder implements BytesEncoder<TopolLink> {
	JSON_V1 {
		@Override
		public Encoding encoding() {
			return Encoding.JSON;
		}

		@Override
		public int sizeInBytes(TopolLink input) {
			return WRITER.sizeInBytes(input);
		}

		@Override
		public byte[] encode(TopolLink link) {
			return JsonCodec.write(WRITER, link);
		}

		@Override
		public byte[] encodeList(List<TopolLink> links) {
			return JsonCodec.writeList(WRITER, links);
		}
	};

	static final Buffer.Writer<TopolLink> WRITER = new Buffer.Writer<TopolLink>() {
		@Override
		public int sizeInBytes(TopolLink value) {
			int sizeInBytes = 58; // {"parent":"","child":"","callCount":,"qps":0.0,"cost":0.0}
			sizeInBytes += jsonEscapedSizeInBytes(value.parent());
			sizeInBytes += jsonEscapedSizeInBytes(value.child());
			sizeInBytes += asciiSizeInBytes(value.callCount());			
			sizeInBytes += jsonEscapedSizeInBytes(String.valueOf(value.qps()));
			sizeInBytes += jsonEscapedSizeInBytes(String.valueOf(value.cost()));
			if (value.errorCount() > 0) {
				sizeInBytes += 14; // ,"errorCount":
				sizeInBytes += asciiSizeInBytes(value.errorCount());
			}
			return sizeInBytes;
		}

		@Override
		public void write(TopolLink value, Buffer b) {
			b.writeAscii("{\"parent\":\"").writeUtf8(jsonEscape(value.parent()));
			b.writeAscii("\",\"child\":\"").writeUtf8(jsonEscape(value.child()));
			b.writeAscii("\",\"callCount\":").writeAscii(value.callCount());
			b.writeAscii(",\"qps\":").writeAscii(String.valueOf(value.qps()));
			b.writeAscii(",\"cost\":").writeAscii(String.valueOf(value.cost()));
			if (value.errorCount() > 0) {
				b.writeAscii(",\"errorCount\":").writeAscii(value.errorCount());
			}
			b.writeByte('}');
		}

		@Override
		public String toString() {
			return "TopolLink";
		}
	};
}
