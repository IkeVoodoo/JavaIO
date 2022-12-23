package me.ikevoodoo.jio.folders;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Stream;

public final class Folders {

    private Folders() {

    }

    /**
     * Clones the contents of a folder into another folder
     * */
    public static void cloneContents(File original, File destination, FolderOption... copyOptions) throws IOException {
        Folders.ensureFolder(original);
        Folders.ensureFolder(destination);

        Path originalPath = original.toPath();
        Path destinationPath = destination.toPath();

        List<FolderOption> options = Arrays.asList(copyOptions);
        if (options.contains(FolderOption.USE_RECURSION)) {
            Files.walkFileTree(originalPath, new FileVisitor<Path>() {
                        @Override
                        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                            Path relative = dir.relativize(originalPath.getFileName());
                            Path destination = destinationPath.resolve(relative);

                            Files.copy(dir, destination);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Path relative = file.relativize(originalPath.getFileName());
                            Path destination = destinationPath.resolve(relative);

                            Files.copy(file, destination);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            return FileVisitResult.CONTINUE;
                        }
                    });
            return;
        }

        try(Stream<Path> paths = Files.walk(originalPath, FileVisitOption.FOLLOW_LINKS)) {
            for (Iterator<Path> it = paths.iterator(); it.hasNext();) {
                Path path = it.next();
                Path relative = path.relativize(originalPath.getFileName());
                Path dest = destinationPath.resolve(relative);

                Files.copy(path, dest);
            }
        }

    }

    /**
     * Clears all content in a folder
     *
     * @param original The folder to clea.r
     * @param clearOptions Options for clearing
     * @return A list of files that weren't deleted.
     * */
    public static List<File> clearFolder(File original, FolderOption... clearOptions) throws IOException {
        Folders.ensureFolder(original);
        List<File> unableToDelete = new ArrayList<>();
        List<FolderOption> options = Arrays.asList(clearOptions);
        Files.walkFileTree(original.toPath(), EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, new FileVisitor<Path>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                File folder = dir.toFile();
                if (!options.contains(FolderOption.USE_RECURSION)) {
                    if (!folder.delete()) {
                        unableToDelete.add(folder);
                    }

                    return FileVisitResult.SKIP_SUBTREE;
                }

                File[] files = folder.listFiles();
                if (files == null || files.length == 0) return FileVisitResult.SKIP_SUBTREE;
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                File f = file.toFile();
                if(!f.delete()) {
                    unableToDelete.add(f);
                }

                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                File folder = dir.toFile();

                if (!folder.delete()) {
                    unableToDelete.add(folder);
                }
                return FileVisitResult.CONTINUE;
            }
        });

        return unableToDelete;
    }

    public static void ensureFolder(File original) {
        if (!original.isDirectory()) {
            throw new IllegalArgumentException(String.format("File at path '%s' is not a directory!", original.getAbsolutePath()));
        }
    }

}
