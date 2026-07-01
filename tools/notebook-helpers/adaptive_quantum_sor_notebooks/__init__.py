"""
Purpose: Exports notebook helper utilities for Adaptive Quantum SOR research workflows.
Usage: Import these helpers from notebooks that talk to the test server or HTTP control plane.
"""

__all__ = [
    "LiveMonitorInputs",
    "LiveSnapshot",
    "LiveStatsDashboard",
    "ScenarioReportApp",
    "ScenarioReportResult",
    "ScenarioRunInputs",
    "SorNotebookClient",
    "SubmitOrderInputs",
    "SubmitOrderReportResult",
    "SubmitParentOrderApp",
]

_MODULE_BY_EXPORT = {
    "SorNotebookClient": "notebook_client",
    "LiveMonitorInputs": "live_monitor",
    "LiveSnapshot": "live_monitor",
    "LiveStatsDashboard": "live_monitor",
    "ScenarioReportApp": "scenario_report",
    "ScenarioReportResult": "scenario_report",
    "ScenarioRunInputs": "scenario_report",
    "SubmitOrderInputs": "submit_order_report",
    "SubmitOrderReportResult": "submit_order_report",
    "SubmitParentOrderApp": "submit_order_report",
}


def __getattr__(name: str):
    module_name = _MODULE_BY_EXPORT.get(name)
    if module_name is None:
        raise AttributeError(f"module {__name__!r} has no attribute {name!r}")
    from importlib import import_module

    module = import_module(f"{__name__}.{module_name}")
    return getattr(module, name)
