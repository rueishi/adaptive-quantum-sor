# Integrating Over Aeron

Run the `sor-test-server` sample server out of process with `--transport=aeron` and connect Java
clients with `AeronSorClient.connect("aeron:ipc", SorClientConfig.defaults())`
or an `aeron:udp?endpoint=host:port` channel.

For low-latency deployments, pin the server and Aeron media driver to isolated
cores with `taskset`, keep noisy workloads off the NUMA node with `numactl`,
and reserve CPU cores at the Kubernetes node level. Use `/healthz`, `/ready`,
and `/metrics` for operations; use Aeron for order flow.
