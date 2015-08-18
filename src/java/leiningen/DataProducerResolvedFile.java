package leiningen;

import org.vafer.jdeb.DataConsumer;
import org.vafer.jdeb.DataProducer;
import org.vafer.jdeb.mapping.Mapper;
import org.vafer.jdeb.producers.AbstractDataProducer;
import org.vafer.jdeb.shaded.compress.compress.archivers.tar.TarArchiveEntry;
import org.vafer.jdeb.utils.FilteredFile;
import org.vafer.jdeb.utils.VariableResolver;

import java.io.*;

/**
 * @author rewe.ischerer
 */
public class DataProducerResolvedFile extends AbstractDataProducer implements DataProducer {
    private final File file;

    private final String destinationName;

    private final VariableResolver resolver;
    private boolean isConfig;

    public DataProducerResolvedFile( final File pFile, String pDestinationName, String[] pIncludes, String[] pExcludes, Mapper[] pMapper, VariableResolver resolver, boolean isConfig) {
        super(pIncludes, pExcludes, pMapper);
        file = pFile;
        destinationName = pDestinationName;
        this.resolver = resolver;
        this.isConfig = isConfig;
    }

    public void produce( final DataConsumer pReceiver ) throws IOException {
        String fileName;
        if (destinationName != null && destinationName.trim().length() > 0) {
            fileName = destinationName.trim();
        } else {
            fileName = file.getName();
        }

        TarArchiveEntry entry = new TarArchiveEntry(fileName, true);
        entry.setUserId(0);
        entry.setUserName("root");
        entry.setGroupId(0);
        entry.setGroupName("root");
        entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE);

        entry = map(entry);

        final InputStream inputStream;
        if(isConfig) {
            FilteredFile ffile = new FilteredFile(new FileInputStream(file), resolver);
            byte[] bytes = ffile.toString().getBytes();
            entry.setSize(bytes.length);
            inputStream = new ByteArrayInputStream(bytes);
        } else {
            entry.setSize(file.length());
            inputStream = new FileInputStream(file);
        }
        try {
            pReceiver.onEachFile(inputStream, entry.getName(), entry.getLinkName(), entry.getUserName(), entry.getUserId(), entry.getGroupName(), entry.getGroupId(), entry.getMode(), entry.getSize());
        } finally {
            inputStream.close();
        }
    }
}
