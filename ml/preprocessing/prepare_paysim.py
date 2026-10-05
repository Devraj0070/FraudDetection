import pandas as pd

INPUT_FILE = "ml/dataset/paysim.csv"
OUTPUT_FILE = "ml/dataset/paysim_prepared.csv"

columns = [
    "step",
    "type",
    "amount",
    "oldbalanceOrg",
    "newbalanceOrig",
    "oldbalanceDest",
    "newbalanceDest",
    "isFraud"
]

print("Loading PaySim dataset...")

df = pd.read_csv(
    INPUT_FILE,
    usecols=columns
)

# Convert transaction type into numeric columns.
df = pd.get_dummies(
    df,
    columns=["type"],
    dtype=int
)

df.to_csv(
    OUTPUT_FILE,
    index=False
)

print("PaySim dataset prepared successfully.")
print("Rows:", len(df))
print("Columns:", len(df.columns))
print("Output:", OUTPUT_FILE)