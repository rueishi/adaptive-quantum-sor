"""
Purpose: Provides the slippage research model implementation or package export.
Usage: Use it from training scripts and research notebooks when fitting or scoring SOR model signals.
"""

def train(rows):
    values = [int(row["label_slippage_bps"]) for row in rows]
    return {"slippage_bps": round(sum(values) / len(values))}


def predict(model, row):
    return int(model["slippage_bps"])
