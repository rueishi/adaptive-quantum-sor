"""
Purpose: Provides the toxicity research model implementation or package export.
Usage: Use it from training scripts and research notebooks when fitting or scoring SOR model signals.
"""

def train(rows):
    values = [int(row["label_toxicity_bps"]) for row in rows]
    return {"toxicity_bps": round(sum(values) / len(values))}


def predict(model, row):
    return int(model["toxicity_bps"])
