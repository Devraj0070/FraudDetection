import pandas as pd

INPUT_FILE = "ml/dataset/paysim.csv"
OUTPUT_FILE = "ml/dataset/paysim_realistic_test.csv"

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

print("Loading PaySim...")

df = pd.read_csv(
    INPUT_FILE,
    usecols=columns
)

# Keep every fraud transaction
fraud = df[df["isFraud"] == 1]

# Keep 20 legitimate transactions for every fraud transaction
legitimate = df[df["isFraud"] == 0].sample(
    n=len(fraud) * 20,
    random_state=42
)

test = pd.concat(
    [fraud, legitimate]
)

# Shuffle
test = test.sample(
    frac=1,
    random_state=42
).reset_index(drop=True)

# Convert transaction type to numeric columns
test = pd.get_dummies(
    test,
    columns=["type"],
    dtype=int
)

# Make sure isFraud is the LAST column
feature_columns = [
    column
    for column in test.columns
    if column != "isFraud"
]

test = test[
    feature_columns + ["isFraud"]
]

test.to_csv(
    OUTPUT_FILE,
    index=False
)

print("================================")
print("REALISTIC TEST DATASET CREATED")
print("================================")
print("Fraud:", len(fraud))
print("Legitimate:", len(legitimate))
print("Total:", len(test))
print("Output:", OUTPUT_FILE)