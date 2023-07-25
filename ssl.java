import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class CustomHttpClient {

    public static CloseableHttpClient createHttpClientWithSslCertificate(String certFilePath, String certPassword) throws Exception {
        // Load the SSL certificate from the file
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(certFilePath)) {
            keyStore.load(fis, certPassword.toCharArray());
        }

        // Create SSL context with the custom certificate
        SSLContext sslContext = SSLContexts.custom()
                .loadKeyMaterial(keyStore, certPassword.toCharArray())
                .build();

        // Set up HttpClient with the custom SSL context
        return HttpClients.custom()
                .setSslcontext(sslContext)
                .build();
    }

    public static void main(String[] args) throws Exception {
        String certFilePath = "/path/to/custom_certificate.p12";
        String certPassword = "your_certificate_password";

        try (CloseableHttpClient httpClient = createHttpClientWithSslCertificate(certFilePath, certPassword)) {
            // Replace "https://example.com/api" with your API endpoint
            HttpGet httpGet = new HttpGet("https://example.com/api");

            // Execute the request
            HttpResponse response = httpClient.execute(httpGet);

            // Process the response
            HttpEntity entity = response.getEntity();
            String responseBody = EntityUtils.toString(entity);
            System.out.println("Response: " + responseBody);
        }
    }
}

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class CustomHttpClientDataflow {

    public static class CustomHttpClientDoFn extends DoFn<String, String> {
        private final String certFilePath;
        private final String certPassword;

        public CustomHttpClientDoFn(String certFilePath, String certPassword) {
            this.certFilePath = certFilePath;
            this.certPassword = certPassword;
        }

        @ProcessElement
        public void processElement(@Element String url, OutputReceiver<String> out) throws Exception {
            try (CloseableHttpClient httpClient = CustomHttpClient.createHttpClientWithSslCertificate(certFilePath, certPassword)) {
                HttpGet httpGet = new HttpGet(url);
                HttpResponse response = httpClient.execute(httpGet);
                String responseBody = EntityUtils.toString(response.getEntity());
                out.output(responseBody);
            }
        }
    }

    public static void main(String[] args) {
        Pipeline pipeline = Pipeline.create();

        // Read URLs from a file
        PCollection<String> urls = pipeline.apply(TextIO.read().from("/path/to/urls.txt"));

        // Use ParDo with CustomHttpClientDoFn to make HTTP requests
        PCollection<String> responses = urls.apply(ParDo.of(new CustomHttpClientDoFn("/path/to/custom_certificate.p12", "your_certificate_password")));

        // Output the responses
        responses.apply(TextIO.write().to("/path/to/responses.txt").withoutSharding());

        pipeline.run().waitUntilFinish();
    }
}
