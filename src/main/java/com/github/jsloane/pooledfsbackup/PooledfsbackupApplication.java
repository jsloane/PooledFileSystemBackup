package com.github.jsloane.pooledfsbackup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.github.jsloane.pooledfsbackup.service.BackupService;

@SpringBootApplication
public class PooledfsbackupApplication implements ApplicationRunner {

	private static final Logger logger = LoggerFactory.getLogger(PooledfsbackupApplication.class);

	private final BackupService backupService;

	public PooledfsbackupApplication(BackupService backupService) {
		this.backupService = backupService;
	}

	public static void main(String[] args) {
		SpringApplication.run(PooledfsbackupApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments args) throws Exception {

		logger.info("Arguments: " + args.getNonOptionArgs());

		boolean execute = false;

		for (String argument : args.getNonOptionArgs()) {
			if ("execute".equalsIgnoreCase(argument)) {
				execute = true;
			}
		}

		if (execute) {
			backupService.runBackup();
		} else {
			logger.warn("Execute argument not provided, skipping backup.");
		}

	}

}
