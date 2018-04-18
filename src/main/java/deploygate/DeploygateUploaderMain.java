package deploygate;

import java.io.File;

public class DeploygateUploaderMain {
    /**
     * Useful for testing
     * @param args Command line arguments
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        DeploygateUploader uploader = new DeploygateUploader();
        DeploygateUploader.UploadRequest r = new DeploygateUploader.UploadRequest();
        r.apiToken = args[0];
        r.buildNotes = args[1];
        File file = new File(args[2]);
        r.file = file;

        uploader.upload(r);
    }
}
