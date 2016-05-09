package uk.nhs.ciao.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class FileUtils {
	
	/**
     * @param file File to load content from
     * @return String containing content of specified file
     */
    public static String loadFile(final File file) {
        String content = null;
        FileReader fr = null;
        try {
            fr = new FileReader(file);
            ByteArrayOutputStream bOutStream = new ByteArrayOutputStream();
            int c = -1;
            while ((c = fr.read()) > -1) {
                bOutStream.write(c);
            }
            content = bOutStream.toString();
        } catch (IOException ex) {
            
        } finally {
            try {
                if (fr != null) fr.close();
            } catch (IOException ex) { }
        }
        return content;
    }
	
	public static void copyFile(File sourceFile, File destFile) throws IOException {
	    if(!destFile.exists()) {
	        destFile.createNewFile();
	    }

	    FileChannel source = null;
	    FileChannel destination = null;

	    try {
	        source = new FileInputStream(sourceFile).getChannel();
	        destination = new FileOutputStream(destFile).getChannel();
	        destination.transferFrom(source, 0, source.size());
	    }
	    finally {
	        if(source != null) {
	            source.close();
	        }
	        if(destination != null) {
	            destination.close();
	        }
	    }
	}
}
