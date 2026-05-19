def train(rows):
    values = [int(row["label_fill_bps"]) for row in rows]
    return {"fill_probability_bps": round(sum(values) / len(values))}


def predict(model, row):
    return int(model["fill_probability_bps"])
