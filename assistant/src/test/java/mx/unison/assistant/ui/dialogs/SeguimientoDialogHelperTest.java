package mx.unison.assistant.ui.dialogs;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SeguimientoDialogHelperTest {

    @TempDir
    Path tempDir;

    @Test
    void testEvitaAgregarElMismoArchivoDosVeces() {
        List<File> stagedFiles = new ArrayList<>();
        File firstFile = tempDir.resolve("constancia.pdf").toFile();
        File sameFileRelative = tempDir.resolve("./constancia.pdf").toFile();
        File secondFile = tempDir.resolve("oficio.pdf").toFile();

        assertTrue(SeguimientoDialog.addUniqueStagedFiles(stagedFiles, new File[] { firstFile }));
        assertFalse(SeguimientoDialog.addUniqueStagedFiles(stagedFiles, new File[] { firstFile, sameFileRelative }));
        assertTrue(SeguimientoDialog.addUniqueStagedFiles(stagedFiles, new File[] { sameFileRelative, secondFile }));

        assertEquals(2, stagedFiles.size());
        assertEquals(firstFile, stagedFiles.get(0));
        assertEquals(secondFile, stagedFiles.get(1));
    }
}
