def train(rows):
    values = [int(row["label_toxicity_bps"]) for row in rows]
    return {"toxicity_bps": round(sum(values) / len(values))}


def predict(model, row):
    return int(model["toxicity_bps"])
