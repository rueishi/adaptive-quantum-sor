from collections import Counter


def train(rows):
    counts = Counter(int(row["label_regime_id"]) for row in rows)
    regime_id = counts.most_common(1)[0][0]
    return {"regime_id": regime_id}


def predict(model, row):
    return int(model["regime_id"])
