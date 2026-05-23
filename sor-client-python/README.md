# Adaptive Quantum SOR Python Client

```python
from adaptive_quantum_sor_client import SorClient

client = SorClient("http://127.0.0.1:8080")
order = client.submit_parent_order(instrument_id=7, side=1, quantity=1000, urgency_id=2)
status = client.get_status(order.parent_order_id)
```

The package wraps the existing HTTP control-plane endpoints used by notebooks:
`POST /orders`, `GET /orders/{id}`, `GET /policy/current`, and
`GET /events/stream`.
