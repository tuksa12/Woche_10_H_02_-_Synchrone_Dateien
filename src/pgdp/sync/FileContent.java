package pgdp.sync;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

final class FileContent {

	private final Instant lastModifiedTime;
	private final List<String> content;

	FileContent(Instant lastModifiedTime, List<String> content) {
		this.lastModifiedTime = Objects.requireNonNull(lastModifiedTime);
		this.content = List.copyOf(content);
	}

	Instant getLastModifiedTime() {
		return lastModifiedTime;
	}

	List<String> getLines() {
		return content;
	}
}
