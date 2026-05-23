"""Exceptions raised by the Adaptive Quantum SOR Python client."""


class SorClientError(RuntimeError):
    """Base exception for client-side failures."""


class SorClientHttpError(SorClientError):
    """Raised when the server returns a non-retryable HTTP error."""

    def __init__(self, status_code: int, body: str) -> None:
        super().__init__(f"HTTP {status_code}: {body}")
        self.status_code = status_code
        self.body = body


class SorClientRetryExhaustedError(SorClientError):
    """Raised when transient HTTP failures exceed the retry budget."""

    def __init__(self, attempts: int) -> None:
        super().__init__(f"retry attempts exhausted after {attempts} attempts")
        self.attempts = attempts
