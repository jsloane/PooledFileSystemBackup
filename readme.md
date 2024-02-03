# Prerequisites
- Maven
- Java 21

# Compiling
Compile with maven:
`mvn clean package`

# Running
Run with command, changing parameters where appropriate:

`java -jar target/pooledfilesystembackup.jar  execute --app.pooledFilesystemMount=/mnt/pooled/ --app.pooledBackupRelativeDirectory=backups/pooled-filesystem-backup --app.backupMarkRelativeDirectory=marks --app.backupRelativeDirectory=backups --app.sourceFilesystemPaths=/mnt/drive01,/mnt/drive02,/mnt/drive03,/mnt/drive04 --app.lookupFilesystemPaths=false`

## Parameters
 - **app.pooledFilesystemMount** - The directory where the pooled file system is mounted.
 - **app.pooledBackupRelativeDirectory** - The relative directory from app.pooledFilesystemMount for where to store backups.
 - **app.backupMarkRelativeDirectory** - The relative directory from app.pooledBackupRelativeDirectory for marking files to backup. Files (symlinks) in this directory will be backed up. Symlinks need to be relative from the pooled file system mount.
 - **app.backupRelativeDirectory** - The relative directory from app.pooledBackupRelativeDirectory for where to store backups.
 - **app.sourceFilesystemPaths** - The mount directory for file systems in the pooled file system.

## Marking files for backup

Example symlink to mark directory to be included in backup:
`ln -s /mnt/pooled/photos /mnt/pooled/backups/pooled-filesystem-backup/marks/photos`
