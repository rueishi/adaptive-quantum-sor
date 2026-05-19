"""Notebook and pandas helpers for Adaptive Quantum SOR."""

from .client import SorNotebookClient
from .dataframe import (
    read_feature_dataframe,
    records_to_dataframe,
    validate_feature_dataframe,
    validate_feature_records,
    write_feature_dataframe,
    write_feature_dataset,
    write_model_predictions_artifact,
)
from .schema import FEATURE_COLUMNS, FEATURE_SCHEMA_VERSION, ORDER_COLUMNS, PREDICTION_COLUMNS

_SCENARIO_CATALOG_EXPORTS = {
    "Scenario",
    "find_scenario",
    "format_scenarios",
    "load_scenarios",
    "parent_order_suggestions",
    "search_scenarios",
}

__all__ = [
    "FEATURE_COLUMNS",
    "FEATURE_SCHEMA_VERSION",
    "ORDER_COLUMNS",
    "PREDICTION_COLUMNS",
    "Scenario",
    "SorNotebookClient",
    "find_scenario",
    "format_scenarios",
    "load_scenarios",
    "parent_order_suggestions",
    "read_feature_dataframe",
    "records_to_dataframe",
    "search_scenarios",
    "validate_feature_dataframe",
    "validate_feature_records",
    "write_feature_dataframe",
    "write_feature_dataset",
    "write_model_predictions_artifact",
]


def __getattr__(name: str):
    if name in _SCENARIO_CATALOG_EXPORTS:
        from . import scenario_catalog

        return getattr(scenario_catalog, name)
    raise AttributeError(f"module 'adaptive_quantum_sor' has no attribute {name!r}")
