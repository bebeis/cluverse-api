package cluverse.post.service.implement;

import java.nio.file.Path;

public final class TemporaryPostImageFile implements AutoCloseable {

    private final Path path;
    private final PostImageUploadTemporaryFileCleaner cleaner;

    TemporaryPostImageFile(Path path, PostImageUploadTemporaryFileCleaner cleaner) {
        this.path = path;
        this.cleaner = cleaner;
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() {
        cleaner.delete(path);
    }
}
