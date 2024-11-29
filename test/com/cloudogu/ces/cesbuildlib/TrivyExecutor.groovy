package com.cloudogu.ces.cesbuildlib

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream

import java.nio.channels.Channels
import java.nio.channels.FileChannel
import java.nio.channels.ReadableByteChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.logging.Logger

class TrivyExecutor {

    private static final Logger logger = Logger.getLogger(TrivyExecutor.class.getName())
    private Path installDir

    TrivyExecutor(Path installDir = Paths.get("trivyInstallation")) {
        this.installDir = installDir
    }

    Process exec(String version, String argumentstring, Path workDir) {
        Path trivyPath = installTrivy(version)
        if (workDir.getParent() != null) {
            Files.createDirectories(workDir.getParent())
        }

        List<String> arguments = new ArrayList()
        arguments.add(trivyPath.toAbsolutePath().toString())
        arguments.addAll(argumentstring.split(" "))
        logger.info("start trivy: ${arguments.join(" ")}")
        return new ProcessBuilder(arguments)
            .directory(workDir.toAbsolutePath().toFile())
            .inheritIO()
            .start()
    }

    /**
     * downloads, extracts and installs trivy as an executable file.
     * Trivy is not downloaded again if the given version is already present.
     * Each trivy version is installed into its own subdirectory to distinguish them.
     * @param version trivy version
     * @return the path to the trivy executable
     */
    private Path installTrivy(String version) {
        Path pathToExtractedArchive = installDir.resolve("v${version}")
        Path pathToTrivyExecutable = pathToExtractedArchive.resolve("trivy")
        if (!pathToExtractedArchive.toFile().exists()) {
            installDir.toFile().mkdirs()
            File archive = downloadTrivy(version, installDir)
            untar(archive, pathToExtractedArchive)
            logger.info("delete trivy download archive $pathToExtractedArchive")
            if (!archive.delete()) {
                throw new RuntimeException("cannot delete trivy download archive: $pathToExtractedArchive")
            }

            logger.fine("make $pathToTrivyExecutable an executable")
            if (pathToTrivyExecutable.toFile().setExecutable(true)) {
                return pathToTrivyExecutable
            } else {
                throw new RuntimeException("cannot make trivy executable: ${pathToTrivyExecutable}")
            }
        } else {
            logger.info("trivy v${version} already installed")
        }

        return pathToTrivyExecutable
    }

    private static File downloadTrivy(String version, Path downloadDir) {
        URL url = new URL("https://github.com/aquasecurity/trivy/releases/download/v${version}/trivy_${version}_Linux-64bit.tar.gz")
        File archive = downloadDir.resolve("trivy.tar.gz").toFile()
        archive.createNewFile()
        logger.info("download trivy v${version} from $url to $archive")

        ReadableByteChannel readableByteChannel = Channels.newChannel(url.openStream())
        FileOutputStream fileOutputStream = new FileOutputStream(archive)
        FileChannel fileChannel = fileOutputStream.getChannel()
        fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE)
        return archive
    }

    private static void untar(File archive, Path destination) throws IOException {
        BufferedInputStream inputStream = new BufferedInputStream(archive.newInputStream())
        TarArchiveInputStream tar = new TarArchiveInputStream(new GzipCompressorInputStream(inputStream))
        logger.info("untar $archive to $destination")
        try {
            ArchiveEntry entry
            while ((entry = tar.getNextEntry()) != null) {
                Path extractTo = entry.resolveIn(destination)
                logger.info("untar: extract entry to ${extractTo}")
                Files.createDirectories(extractTo.getParent())
                Files.copy(tar, extractTo)
            }
        } finally {
            inputStream.close()
        }
    }
}
