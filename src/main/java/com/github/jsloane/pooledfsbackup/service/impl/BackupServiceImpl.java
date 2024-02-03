package com.github.jsloane.pooledfsbackup.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.github.jsloane.pooledfsbackup.service.BackupService;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@Service("backupService")
public class BackupServiceImpl implements BackupService {

    private static final Logger logger = LoggerFactory.getLogger(BackupServiceImpl.class);

    @Value("${app.pooledFilesystemMount}")
    private String pooledFilesystemMount;

    @Value("${app.pooledBackupRelativeDirectory}")
    private String pooledBackupRelativeDirectory;

    @Value("${app.backupMarkRelativeDirectory}")
    private String backupMarkRelativeDirectory;

    @Value("${app.backupRelativeDirectory}")
    private String backupRelativeDirectory;

    @Value("${app.sourceFilesystemPaths}")
    private List<String> sourceFilesystemPaths;

    @Value("${app.lookupFilesystemPaths}")
    private Boolean lookupFilesystemPaths; // TODO extract from /etc/fstab

    // TODO add restore function
    // TODO add 'add file/directory' function
    // TODO keep x last backups...
    // TODO incremental backups...
    // TODO provide GraalVM build

    @Override
    public void runBackup() {
        String parametersMessage = "\npooledFilesystemMount:         " + pooledFilesystemMount
                + "\npooledBackupRelativeDirectory: " + pooledBackupRelativeDirectory
                + "\nbackupMarkRelativeDirectory:   " + backupMarkRelativeDirectory
                + "\nbackupRelativeDirectory:       " + backupRelativeDirectory
                + "\nsourceFilesystemPaths:         " + sourceFilesystemPaths;
        logger.info("Running with parameters: " + parametersMessage);

        Path pooledFilesystemMountPath = Paths.get(pooledFilesystemMount);
        Path pooledBackupPath = pooledFilesystemMountPath.resolve(pooledBackupRelativeDirectory);
        Path backupMarkPath = pooledBackupPath.resolve(backupMarkRelativeDirectory);
        Path backupPath = pooledBackupPath.resolve(backupRelativeDirectory);

        String pathsMessage = "\npooledFilesystemMountPath: " + pooledFilesystemMountPath
                + "\npooledBackupPath:          " + pooledBackupPath
                + "\nbackupMarkPath:            " + backupMarkPath
                + "\nbackupPath:                " + backupPath;
        logger.info("Using paths: " + pathsMessage);

        NumberFormat nf = NumberFormat.getNumberInstance();

        Map<String, Path> sourceFileSystemPathsMap = new HashMap<>();

        for (String sourceFilesystemPath : sourceFilesystemPaths) {
            Path sourceFileSystemPath = Paths.get(sourceFilesystemPath);
            sourceFileSystemPathsMap.put(sourceFilesystemPath, sourceFileSystemPath);
        }

        List<Path> filesFailedToBackup = new ArrayList<>();

        try {
            try (Stream<Path> markPathStream = Files.walk(backupMarkPath, FileVisitOption.FOLLOW_LINKS)) {
                markPathStream
                    .filter(java.nio.file.Files::isRegularFile)
                    .forEach(file -> {
                        boolean backedUp = false;

                        try {
                            long fileSize = 0;
                            try (FileChannel fileChannel = FileChannel.open(file)) {
                                fileSize = fileChannel.size();
                            }

                            // loop over file systems for backing up to
                            for (String sourceFileSystemPathString : sourceFilesystemPaths) {
                                Path sourceFileSystemPath = sourceFileSystemPathsMap.get(sourceFileSystemPathString);
                                FileStore store = Files.getFileStore(sourceFileSystemPath);

                                // check that file is not stored on this drive
                                Path fileRelativePath = backupMarkPath.relativize(file);
                                Path fileOnFileSystemPath = sourceFileSystemPath.resolve(fileRelativePath);
                                boolean fileOnFileSystem = Files.exists(fileOnFileSystemPath);

                                // check that the drive has enough space to backup the file
                                boolean hasSpaceToCopy = store.getUsableSpace() > fileSize;

                                if (!fileOnFileSystem && hasSpaceToCopy) {
                                    // copy file to this drive

                                    Path backupFile = sourceFileSystemPath.resolve(pooledBackupRelativeDirectory).resolve(backupRelativeDirectory).resolve(fileRelativePath);
                                    Path backupFileDir = backupFile.getParent();

                                    // TODO check if file has changed since last backup, if not skip

                                    logger.info("Backing up file " + file + " to " + backupFile);

                                    if (!Files.exists(backupFileDir)) {
                                        Files.createDirectories(backupFileDir);
                                    }

                                    Files.copy(file, backupFile, StandardCopyOption.REPLACE_EXISTING);

                                    backedUp = true;
                                    break;
                                }
                            }
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }

                        if (!backedUp) {
                            filesFailedToBackup.add(file);
                            logger.error("No drives with enough space to backup file " + file);
                        }
                    });
            }
        } catch (java.io.IOException e) {
            logger.error("Unable to list files in the directory.", e);
        }

        // TODO result summary
    }

}
