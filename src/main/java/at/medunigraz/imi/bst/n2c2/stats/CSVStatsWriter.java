package at.medunigraz.imi.bst.n2c2.stats;

import at.medunigraz.imi.bst.n2c2.model.Criterion;
import at.medunigraz.imi.bst.n2c2.model.metrics.MetricSet;
import com.opencsv.CSVWriter;

import java.io.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class CSVStatsWriter extends AbstractStatsWriter {
    private CSVWriter writer;

    public CSVStatsWriter(OutputStream output) {
        super(output);
        writer = new CSVWriter(new OutputStreamWriter(output, Charset.forName("UTF-8")));
        writeHeader();
    }

    public CSVStatsWriter(File outputFile) throws IOException {
        this(new FileOutputStream(outputFile));
    }

    protected void writeHeader() {
        List<String> metricNames = MetricSet.getMetricNames();

        metricNames.add(0, GROUPED_BY);
        String[] header = metricNames.toArray(new String[metricNames.size()]);

        writer.writeNext(header);
        try {
            flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void write(MetricSet metrics) {
        for (Criterion c : Criterion.values()) {
            // Keys might not properly ordered
            Map<String, Double> metricsMap = metrics.getMetrics(c);

            // Criterion + values
            String[] entries = new String[1 + metricsMap.size()];
            entries[0] = c.name();

            // This is properly ordered
            List<String> metricNames = MetricSet.getMetricNames();
            int i = 0;
            for (String metricName : metricNames) {
                entries[++i] = String.valueOf(metricsMap.get(metricName));
            }

            writer.writeNext(entries);
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    @Override
    public void flush() throws IOException {
        writer.flush();
    }
}
