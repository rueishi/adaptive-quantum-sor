# SOR HTTP Control Plane

This module is research and ops only.

It is not the production order intake path. Production order flow uses Aeron;
this HTTP server exposes liveness, readiness, Prometheus metrics, and OpenAPI
metadata for notebooks and operational inspection.
