# Adaptive Quantum SOR Java Client

```groovy
dependencies {
    implementation 'com.nitroj.sor:sor-client-java:1.0.0'
}
```

```java
import com.nitroj.sor.api.ParentOrderRequest;
import com.nitroj.sor.api.Side;
import com.nitroj.sor.client.AeronSorClient;
import com.nitroj.sor.client.SorClientConfig;

public final class Quickstart {
    public static void main(String[] args) {
        try (AeronSorClient client = AeronSorClient.connect("aeron:ipc", SorClientConfig.defaults())) {
            client.warmup();
            long parentOrderId = client.submit(ParentOrderRequest.builder()
                    .instrumentId(0)
                    .side(Side.BUY)
                    .quantity(100)
                    .urgency(0)
                    .clientOrderId(42)
                    .build());
            System.out.println("submitted parentOrderId=" + parentOrderId);
        }
    }
}
```

The SDK exposes API DTOs from `sor-api`, protocol classes from `sor-codec`, and
the Aeron client transport behind `AeronSorClient.connect(channelUri, config)`.
Use `close()` during shutdown to release the client facade.
