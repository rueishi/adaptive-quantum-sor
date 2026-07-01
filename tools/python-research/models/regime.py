"""
Purpose: Provides the regime research model implementation or package export.
Usage: Use it from training scripts and research notebooks when fitting or scoring SOR model signals.
"""

from collections import Counter


def train(rows):
    counts = Counter(int(row["label_regime_id"]) for row in rows)
    regime_id = counts.most_common(1)[0][0]
    return {"regime_id": regime_id}


def predict(model, row):
    return int(model["regime_id"])
