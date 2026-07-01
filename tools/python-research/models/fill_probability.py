"""
Purpose: Provides the fill probability research model implementation or package export.
Usage: Use it from training scripts and research notebooks when fitting or scoring SOR model signals.
"""

def train(rows):
    values = [int(row["label_fill_bps"]) for row in rows]
    return {"fill_probability_bps": round(sum(values) / len(values))}


def predict(model, row):
    return int(model["fill_probability_bps"])
