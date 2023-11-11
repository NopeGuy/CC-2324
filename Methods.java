import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class Methods {
    public static int fileSplitter(String fileName, String filePath, int numBlocks) {
        String outputDir = "./ClientFiles/";
        int blockSize = 1007; // Tamanho do bloco em bytes

        File inputFile = new File(filePath);

        try (FileInputStream fis = new FileInputStream(inputFile)) {
            byte[] buffer = new byte[blockSize];
            int bytesRead;
            int blockNumber = 1; // Inicia o número do bloco em 1

            long fileSize = inputFile.length(); // Obtém o tamanho real do arquivo

            for (int i = 0; i < numBlocks - 1; i++) {
                bytesRead = fis.read(buffer, 0, blockSize);

                String blockFileName = String.format(outputDir + "%s«%04d", fileName, blockNumber);
                try (FileOutputStream fos = new FileOutputStream(blockFileName)) {
                    fos.write(buffer, 0, bytesRead);
                }

                blockNumber++;
            }

            // Trata o último bloco
            int remainingBytes = (int) (fileSize % blockSize);
            bytesRead = fis.read(buffer, 0, remainingBytes);

            String lastBlockFileName = String.format(outputDir + "%s«%04d", fileName, blockNumber);
            try (FileOutputStream fos = new FileOutputStream(lastBlockFileName)) {
                fos.write(buffer, 0, bytesRead);
            }

            System.out.println("File split into " + numBlocks + " blocks.");

            // Descomente as linhas abaixo se quiser excluir o arquivo original após a
            // fragmentação
            /*
             * if (inputFile.delete()) {
             * System.out.println("Original file deleted successfully.");
             * } else {
             * System.out.println("Failed to delete the original file.");
             * }
             */

            return numBlocks;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return (-1);
    }
}
