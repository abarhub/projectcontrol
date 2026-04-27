package org.projectcontrol.core.utils;

import org.apache.commons.lang3.SystemUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

public class TempUtils {

    private TempUtils() {
        /* This utility class should not be instantiated */
    }

    public static Path createTempFile(String prefix, String suffix) throws IOException {
        if (SystemUtils.IS_OS_UNIX) {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------"));
            return Files.createTempFile(prefix, suffix, attr); // Compliant
        } else {
            File f = Files.createTempFile(prefix, suffix).toFile();  // Compliant
            var res = f.setReadable(true, true);
            if (!res) {
                throw new IOException("impossible de changer le mode de lecture");
            }
            res = f.setWritable(true, true);
            if (!res) {
                throw new IOException("impossible de changer le mode d'ecriture");
            }
            res = f.setExecutable(true, true);
            if (!res) {
                throw new IOException("impossible de changer le mode d'execution");
            }
            return f.toPath();
        }
    }

}
